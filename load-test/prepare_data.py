"""Build synthetic k6 input from an SJA MySQL dump.

The generated file contains access tokens and must never be committed. Run this
only against an isolated database restored from the same SQL snapshot.
"""

from __future__ import annotations

import argparse
import base64
import csv
import hashlib
import hmac
import json
import os
import re
import time
import uuid
from pathlib import Path


def parse_rows(sql: str, table: str) -> list[list[str]]:
    pattern = re.compile(
        rf"^INSERT INTO `{re.escape(table)}` VALUES \((.*)\);$", re.MULTILINE
    )
    return [
        next(
            csv.reader(
                [match.group(1)],
                delimiter=",",
                quotechar="'",
                escapechar="\\",
                skipinitialspace=True,
            )
        )
        for match in pattern.finditer(sql)
    ]


def b64url(value: bytes) -> str:
    return base64.urlsafe_b64encode(value).rstrip(b"=").decode("ascii")


def jwt_for(user_id: int, username: str, token_version: int, secret: str, ttl: int) -> str:
    now = int(time.time())
    header = {"alg": "HS256", "typ": "JWT"}
    payload = {
        "userId": user_id,
        "username": username,
        "tokenVersion": token_version,
        "tokenType": "ACCESS",
        "sub": username,
        "jti": str(uuid.uuid4()),
        "iat": now,
        "exp": now + ttl,
    }
    encoded_header = b64url(json.dumps(header, separators=(",", ":")).encode())
    encoded_payload = b64url(json.dumps(payload, separators=(",", ":")).encode())
    unsigned = f"{encoded_header}.{encoded_payload}"
    signature = hmac.new(secret.encode(), unsigned.encode(), hashlib.sha256).digest()
    return f"{unsigned}.{b64url(signature)}"


def synthetic_profile(row: list[str], token: str) -> dict[str, object]:
    user_id = int(row[0])
    # Keep studentNo unchanged because changing it also changes username and tokenVersion.
    return {
        "token": token,
        "userId": user_id,
        "profile": {
            "realName": f"压测用户{user_id:03d}",
            "gender": int(row[6]) if row[6] in {"0", "1", "2"} else 0,
            "idCard": f"990000200001{user_id:06d}",
            "studentNo": row[9],
            "college": "压测学院",
            "className": "压测班",
            "dormAddress": f"压测宿舍-A-{user_id}",
            "phone": f"199{user_id:08d}",
            "bankCard": f"622202{user_id:010d}",
        },
    }


def fee_template(work_type: str) -> tuple[dict[str, object], str]:
    common = {
        "publishDate": time.strftime("%Y-%m-%d"),
        "isPublished": 1,
        "isSpecialNode": 0,
        "specialMultiplier": 1,
        "remark": "k6并发压力测试",
    }
    if work_type == "PHOTOGRAPHY":
        return ({**common, "title": "k6摄影压测", "platform": "微信", "category": "", "readCount": 1000, "photoCount": 1, "articleLink": ""}, "AUTHOR")
    if work_type == "VISUAL":
        return ({**common, "title": "k6视觉压测", "category": "普通消息类", "authorCount": 1, "articleLink": ""}, "DESIGNER")
    if work_type == "VIDEO":
        return ({**common, "videoTitle": "k6视频压测", "category": "活动新闻类视频", "likeCount": 0, "isReposted": 0, "isComplexEdit": 0, "videoLink": ""}, "SHOOTER")
    return ({**common, "title": "k6采编压测", "platform": "微信", "category": "原创类", "wordCount": 0, "readCount": 1000, "articleLink": ""}, "AUTHOR")


def main() -> None:
    parser = argparse.ArgumentParser()
    parser.add_argument("--sql", required=True, type=Path)
    parser.add_argument("--output", type=Path, default=Path("generated/users.generated.json"))
    parser.add_argument("--ttl-minutes", type=int, default=120)
    args = parser.parse_args()

    secret = os.environ.get("SJA_JWT_SECRET", "")
    if len(secret.encode()) < 32:
        raise SystemExit("SJA_JWT_SECRET must contain at least 32 bytes")

    sql = args.sql.read_text(encoding="utf-8-sig")
    users = parse_rows(sql, "sys_user")
    roles = parse_rows(sql, "sys_role")
    menus = parse_rows(sql, "sys_menu")
    role_menus = parse_rows(sql, "sys_role_menu")
    user_roles = parse_rows(sql, "sys_user_role")
    depts = parse_rows(sql, "sys_dept")
    user_depts = parse_rows(sql, "sys_user_dept")

    valid_users = {int(row[0]): row for row in users if int(row[16]) == 1}
    role_codes = {int(row[0]): int(row[2]) for row in roles}
    permission_menu_ids = {int(row[0]) for row in menus if row[2] == "fee:submit"}
    fee_role_ids = {int(row[1]) for row in role_menus if int(row[2]) in permission_menu_ids}

    roles_by_user: dict[int, set[int]] = {}
    for row in user_roles:
        roles_by_user.setdefault(int(row[1]), set()).add(int(row[2]))

    dept_codes = {int(row[0]): row[2] for row in depts}
    depts_by_user: dict[int, set[str]] = {}
    for row in user_depts:
        depts_by_user.setdefault(int(row[1]), set()).add(dept_codes[int(row[2])])

    ttl = args.ttl_minutes * 60
    tokens: dict[int, str] = {}
    profiles = []
    for user_id, row in sorted(valid_users.items()):
        token = jwt_for(user_id, row[1], int(row[4]), secret, ttl)
        tokens[user_id] = token
        profiles.append(synthetic_profile(row, token))

    fee_users = sorted(
        user_id
        for user_id in valid_users
        if roles_by_user.get(user_id, set()) & fee_role_ids
    )
    fees = []
    work_type_priority = ["EDITORIAL", "PHOTOGRAPHY", "VISUAL", "VIDEO"]
    for user_id in fee_users:
        highest_role_code = min(role_codes[role_id] for role_id in roles_by_user[user_id])
        allowed = set(work_type_priority) if highest_role_code in {0, 1, 2, 3} else depts_by_user.get(user_id, set())
        work_type = next((item for item in work_type_priority if item in allowed), None)
        if work_type is None:
            raise SystemExit(f"fee submitter {user_id} has no supported department")
        work_info, contributor_role = fee_template(work_type)
        fees.append({
            "token": tokens[user_id],
            "userId": user_id,
            "workType": work_type,
            "workInfo": work_info,
            "roleInWork": contributor_role,
        })

    output = {
        "sourceSummary": {
            "totalUsers": len(users),
            "validUsers": len(valid_users),
            "invalidUsers": len(users) - len(valid_users),
            "feeSubmitters": len(fees),
        },
        "profiles": profiles,
        "fees": fees,
    }
    args.output.parent.mkdir(parents=True, exist_ok=True)
    args.output.write_text(json.dumps(output, ensure_ascii=False, indent=2), encoding="utf-8")
    print(json.dumps(output["sourceSummary"], ensure_ascii=False))
    print(f"generated: {args.output.resolve()}")


if __name__ == "__main__":
    main()
