package com.ghostteam.findetect;

import java.math.BigDecimal;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;
import java.time.format.DateTimeFormatterBuilder;
import java.util.*;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import tools.jackson.databind.ObjectMapper;

@RestController
@RequestMapping("/api/v1")
public class FinanceController {
    private record Table(String name, List<String> columns) {
        Table(String name, String fields) { this(name, List.of(fields.split(" "))); }
        String key() { return columns.get(0); }
        String projection() { return String.join(",", columns.stream().map(FinanceController::quote).toList()); }
    }

    private static final List<Table> TABLES = List.of(
            new Table("categories", "id key nameRu nameEn iconId isIncome isArchived legacyEmoji"),
            new Table("accounts", "id name iconId currency initialBalanceMinor modifiedAt includeInForecast createdAt updatedAt deletedAt legacyEmoji"),
            new Table("transactions", "id accountId categoryId amountMinor transactionDate enteredAt modifiedAt comment createdAt updatedAt deletedAt"),
            new Table("balance_adjustments", "id accountId amountMinor occurredAt createdAt"),
            new Table("balance_confirmations", "requestId accountId currency actualMinor adjustmentId"),
            new Table("activity_days", "date hadTransaction noSpendingConfirmed enteredAt modifiedAt"),
            new Table("xp_state", "id startedAt legacyBestStreak"),
            new Table("achievement_state", "id startedAt startedOn zoneId freshStartDate"),
            new Table("achievement_transaction_facts", "transactionId enteredOn transactionOn zoneId enteredAt"),
            new Table("xp_events", "id reason sourceId reasonRef amount counted localDate occurredAt createdAt"),
            new Table("achievements", "code earnedAt earnedOn isLegacy xpEventId createdAt"),
            new Table("forecast_plans", "currency savingsMinor paymentsMinor paymentDate manualDailyMinor modifiedAt"),
            new Table("forecast_days", "id currency day stamp accountIds fromUtc untilUtc limitMinor"),
            new Table("forecast_balances", "id currency stamp localDay targetDay rule accountIds balanceMinor"),
            new Table("personal_settings", "id gameEnabled reminderEnabled reminderHour modifiedAt nextIncomeDate incomeRepeat"));
    private static final Set<String> FLAGS = Set.of("isIncome","isArchived","includeInForecast","hadTransaction",
            "noSpendingConfirmed","counted","isLegacy","gameEnabled","reminderEnabled");
    private static final Set<String> TIMES = Set.of("modifiedAt","createdAt","updatedAt","deletedAt",
            "transactionDate","enteredAt","occurredAt","startedAt","earnedAt","fromUtc","untilUtc");
    private static final java.time.format.DateTimeFormatter UTC = new DateTimeFormatterBuilder().appendInstant(9).toFormatter();
    private final JdbcTemplate db;
    private final AuthController auth;
    private final ObjectMapper json;

    public FinanceController(JdbcTemplate db, AuthController auth, ObjectMapper json) {
        this.db=db;
        this.auth=auth;
        this.json=json;
    }

    record Changes(long revision, Map<String,List<Map<String,String>>> tables) {}

    @PostMapping("/sync")
    @Transactional
    public Changes exchange(@RequestHeader(value="Authorization",required=false) String header, @RequestBody Changes request) {
        UUID owner = auth.owner(header);
        long current = db.queryForObject("SELECT revision FROM users WHERE id=? FOR UPDATE",Long.class,owner);
        if (request.tables()==null || request.revision()<0 || request.revision()>current ||
                !TABLES.stream().map(Table::name).toList().containsAll(request.tables().keySet())) throw invalid();
        long next = current;
        var result = new LinkedHashMap<String,List<Map<String,String>>>();
        for (Table table : TABLES) {
            var rows = request.tables().getOrDefault(table.name(), List.of());
            if (rows==null) throw invalid();
            for (var row : rows) {
                if (row==null || !row.keySet().equals(new HashSet<>(table.columns())) ||
                        row.get(table.key())==null || row.get(table.key()).isBlank()) throw invalid();
                validate(table,row);
                if (write(table,owner,current+1,row)) next=current+1;
            }
            // Echo every submitted ID, including a stale edit whose stored row kept its revision.
            var ids = rows.stream().map(row -> row.get(table.key())).toList();
            List<Map<String,String>> delta = db.query("SELECT "+table.projection()+" FROM "+table.name()+
                    " WHERE user_id=? AND (revision>? OR "+quote(table.key())+
                    "::text IN (SELECT jsonb_array_elements_text(CAST(? AS jsonb)))) ORDER BY "+quote(table.key()),
                    (rs,i)->read(table,rs),owner,request.revision(),json.writeValueAsString(ids));
            result.put(table.name(),delta);
        }
        if (next!=current) db.update("UPDATE users SET revision=? WHERE id=?",next,owner);
        return new Changes(next,result);
    }

    private boolean write(Table t, UUID owner, long next, Map<String,String> input) {
        var row = new LinkedHashMap<>(input);
        if (row.containsKey("createdAt") && row.get("createdAt")==null) row.put("createdAt",UTC.format(Instant.now()));
        if (row.containsKey("updatedAt")) row.put("updatedAt",row.get("modifiedAt"));
        if (t.name().equals("accounts")) validateAccountUpdate(owner,row);
        if (t.name().equals("xp_events")) limitDailyReward(owner,row);
        String fields=t.projection();
        String updates=String.join(",", t.columns().stream().filter(c->!c.equals(t.key()) && !c.equals("createdAt"))
                .map(c->quote(c)+"=excluded."+quote(c)).toList());
        // Immutable journals keep the first receipt. Mutable records compare the captured modification time.
        String conflict="DO NOTHING";
        if (row.containsKey("modifiedAt")) {
            if (t.name().equals("activity_days")) {
                updates="\"hadTransaction\"=greatest(old.\"hadTransaction\",excluded.\"hadTransaction\"),"+
                        "\"noSpendingConfirmed\"=greatest(old.\"noSpendingConfirmed\",excluded.\"noSpendingConfirmed\"),"+
                        "\"enteredAt\"=least(old.\"enteredAt\",excluded.\"enteredAt\"),"+
                        "\"modifiedAt\"=greatest(old.\"modifiedAt\",excluded.\"modifiedAt\")";
                conflict="DO UPDATE SET "+updates+",revision=excluded.revision";
            } else {
                conflict="DO UPDATE SET "+updates+",revision=excluded.revision WHERE old.\"modifiedAt\" <= excluded.\"modifiedAt\"";
            }
        }
        String sql="INSERT INTO "+t.name()+" AS old (user_id,revision,"+fields+") SELECT ?,?,"+fields+
                " FROM jsonb_populate_record(NULL::"+t.name()+",CAST(? AS jsonb)) ON CONFLICT (user_id,"+
                quote(t.key())+") "+conflict;
        try {
            return db.update(sql,owner,next,json.writeValueAsString(row))>0;
        } catch (DataIntegrityViolationException ex) { throw invalid(); }
    }

    private void validateAccountUpdate(UUID owner, Map<String,String> row) {
        var previous = db.queryForList("""
                SELECT currency, "initialBalanceMinor" FROM accounts WHERE user_id=? AND id=?
                    AND "modifiedAt"<=CAST(? AS timestamptz)
                """,owner,row.get("id"),row.get("modifiedAt"));
        if (previous.isEmpty()) return;
        var old = previous.get(0);
        if (!old.get("initialBalanceMinor").toString().equals(row.get("initialBalanceMinor"))) throw invalid();
        if (Objects.equals(old.get("currency"),row.get("currency"))) return;
        boolean used = db.queryForObject("""
                SELECT EXISTS(SELECT 1 FROM transactions WHERE user_id=? AND "accountId"=?)
                    OR EXISTS(SELECT 1 FROM balance_adjustments WHERE user_id=? AND "accountId"=?)
                """,Boolean.class,owner,row.get("id"),owner,row.get("id"));
        if (used) throw invalid();
    }

    private void limitDailyReward(UUID owner, Map<String,String> row) {
        String reason=row.get("reason");
        if (!"TRANSACTION".equals(reason) && !"NO_SPENDING_MARK".equals(reason)) return;
        int limit="TRANSACTION".equals(reason) ? 5 : 1;
        int points="TRANSACTION".equals(reason) ? 10 : 5;
        int previous=db.queryForObject("""
                SELECT count(*) FROM xp_events WHERE user_id=? AND reason=? AND "localDate"=CAST(? AS date)
                AND counted AND id<>?
                """,Integer.class,owner,reason,row.get("localDate"),row.get("id"));
        boolean counted=previous<limit && "1".equals(row.get("counted"));
        row.put("counted",counted ? "1" : "0");
        row.put("amount",counted ? Integer.toString(points) : "0");
    }

    private static void validate(Table t, Map<String,String> row) {
        for (String c : TIMES) {
            String value=row.get(c);
            if (value!=null) {
                try { Instant.parse(value); } catch (java.time.DateTimeException ex) { throw invalid(); }
            }
        }
        if (row.get("zoneId") != null) {
            try { java.time.ZoneId.of(row.get("zoneId")); } catch (java.time.DateTimeException error) { throw invalid(); }
        }
        if (t.name().equals("accounts") && (row.get("name")==null || row.get("name").isBlank())) throw invalid();
    }

    private static Map<String,String> read(Table t,ResultSet rs) throws SQLException {
        Map<String,String> result = new LinkedHashMap<>();
        for (String c : t.columns()) {
            String value=rs.getString(c);
            if (value!=null && FLAGS.contains(c)) value=rs.getBoolean(c) ? "1" : "0";
            if (value!=null && TIMES.contains(c)) value=UTC.format(rs.getTimestamp(c).toInstant());
            result.put(c,value);
        }
        return result;
    }

    @GetMapping("/accounts")
    public Map<String,Object> accounts(@RequestHeader(value="Authorization",required=false) String header) {
        UUID owner=auth.owner(header);
        Table table=TABLES.stream().filter(t->t.name().equals("accounts")).findFirst().orElseThrow();
        var items=db.query("SELECT "+table.projection()+" FROM accounts WHERE user_id=? AND \"deletedAt\" IS NULL ORDER BY name,id",
                (rs,i)->{
                    var row=new LinkedHashMap<String,Object>(read(table,rs));
                    row.put("initialBalance",BigDecimal.valueOf(rs.getLong("initialBalanceMinor"),2).toPlainString());
                    row.remove("initialBalanceMinor");
                    row.put("includeInForecast",rs.getBoolean("includeInForecast"));
                    return row;
                },owner);
        Map<String,Object> page=new LinkedHashMap<>();
        page.put("items",items);
        page.put("nextCursor",null);
        return page;
    }

    private static String quote(String knownColumn) { return "\""+knownColumn+"\""; }
    private static ResponseStatusException invalid() { return new ResponseStatusException(HttpStatus.BAD_REQUEST); }
}
