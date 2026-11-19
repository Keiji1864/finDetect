"""Run against the local server: python backend/check_api.py"""
import json
import secrets
import uuid
from urllib.error import HTTPError
from urllib.request import Request, urlopen

BASE = "http://127.0.0.1:8080/api/v1"


def call(method, path, body=None, token=None, expected=200):
    headers = {"Content-Type": "application/json"}
    if token:
        headers["Authorization"] = "Bearer " + token
    request = Request(BASE + path, data=json.dumps(body).encode() if body is not None else None,
                      headers=headers, method=method)
    try:
        response = urlopen(request, timeout=20)
    except HTTPError as error:
        response = error
    with response:
        raw = response.read()
        if response.status != expected:
            raise AssertionError(f"{method} {path}: {response.status} (expected {expected})")
        return json.loads(raw) if raw else None


def account():
    email = f"check-{uuid.uuid4()}@example.org"
    password = secrets.token_urlsafe(24)
    auth = call("POST", "/auth/register", {"email": email, "name": "Вилена", "password": password}, expected=201)
    return email, password, auth


def run():
    users = []
    try:
        for _ in range(2):
            users.append(account())
        email, password, first = users[0]
        token = first["tokens"]["accessToken"]
        other = users[1][2]["tokens"]["accessToken"]
        assert call("GET", "/me", token=token)["name"] == "Вилена"
        call("GET", "/me", expected=401)
        call("POST", "/auth/login", {"email": email, "password": "incorrect"}, expected=401)
        call("POST", "/auth/register", {"email": email.upper(), "name": "Тест", "password": password}, expected=409)
        assert call("PATCH", "/me", {"name": "Новое имя"}, token)["name"] == "Новое имя"
        assert call("GET", "/me", token=other)["id"] != first["user"]["id"]
        stamp = "2026-11-18T10:00:00.000000000Z"
        aid, cid, tid = (str(uuid.uuid4()) for _ in range(3))
        wallet = dict(id=aid, name="Основной", iconId="wallet", currency="RUB", initialBalanceMinor="10000",
                      modifiedAt=stamp, includeInForecast="1", createdAt=None, updatedAt=None, deletedAt=None, legacyEmoji=None)
        food = dict(id=cid, key="food", nameRu="Продукты", nameEn="Food", iconId="basket",
                    isIncome="0", isArchived="0", legacyEmoji=None)
        expense = dict(id=tid, accountId=aid, categoryId=cid, amountMinor="2550", transactionDate=stamp,
                       enteredAt=stamp, modifiedAt=stamp, comment="Обед", createdAt=None, updatedAt=None, deletedAt=None)
        payload = dict(revision=0, tables=dict(accounts=[wallet], categories=[food], transactions=[expense]))
        call("POST", "/sync", payload, token)
        again = call("POST", "/sync", payload, token)
        assert len(again["tables"]["transactions"]) == 1
        assert call("POST", "/sync", {"revision": 0, "tables": {}}, other)["tables"]["accounts"] == []
        call("POST", "/sync", {"revision": 0, "tables": {"transactions": [expense]}}, other, 400)
        assert call("GET", "/accounts", token=token)["items"][0]["initialBalance"] == "100.00"
        call("POST", "/auth/logout", {"refreshToken": first["tokens"]["refreshToken"]}, expected=204)
        call("GET", "/me", token=token, expected=401)
        auth = call("POST", "/auth/login", {"email": email, "password": password})
        rotated = call("POST", "/auth/refresh", {"refreshToken": auth["tokens"]["refreshToken"]})
        call("POST", "/auth/refresh", {"refreshToken": auth["tokens"]["refreshToken"]}, expected=401)
        token = rotated["accessToken"]
        assert call("GET", "/me", token=token)["name"] == "Новое имя"
        restored = call("POST", "/sync", {"revision": 0, "tables": {}}, token)
        assert restored["tables"]["transactions"][0]["amountMinor"] == "2550"
        assert restored["tables"]["accounts"][0]["id"] == aid
        assert all(not rows for rows in call("POST", "/sync", {"revision": restored["revision"], "tables": {}}, token)["tables"].values())
        call("DELETE", "/me", token=token, expected=204)
        call("GET", "/me", token=token, expected=401)
        call("POST", "/auth/login", {"email": email, "password": password}, expected=401)
        users.pop(0)
        print("OK: auth, profile, finance, retry, isolation, restore, logout and delete")
    finally:
        for email, password, _ in users:
            session = call("POST", "/auth/login", {"email": email, "password": password})
            call("DELETE", "/me", token=session["tokens"]["accessToken"], expected=204)


if __name__ == "__main__":
    run()
