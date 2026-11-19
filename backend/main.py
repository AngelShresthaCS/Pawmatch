"""Petinder local MVP API. Run: python -m uvicorn backend.main:app --reload."""
from contextlib import asynccontextmanager, contextmanager
from datetime import datetime, timezone
import hashlib
import math
import os
from pathlib import Path
import secrets
import sqlite3
from typing import Annotated, Literal

from fastapi import Depends, FastAPI, HTTPException, Query
from fastapi.security import HTTPAuthorizationCredentials, HTTPBearer
from pydantic import BaseModel, Field, field_validator

DB_PATH = Path(os.environ.get("PETINDER_DB", Path(__file__).with_name("petinder.db")))
security = HTTPBearer(auto_error=False)

PETS = [
    ("buddy", "Buddy", "Dog", "Golden Retriever", 2, "Male", "Large", "buddy", 41.891, -87.628,
     "Chicago, IL", "PAWS Chicago", "A sunny soul with a very waggy tail. Buddy loves long walks, tennis balls, and being right by your side.", "Playful,Gentle,Good with kids", 250),
    ("luna", "Luna", "Cat", "Domestic Shorthair", 1, "Female", "Small", "luna", 41.921, -87.651,
     "Chicago, IL", "Tree House Humane Society", "Your next windowsill companion. Luna is curious, affectionate, and always ready for a quiet afternoon together.", "Curious,Affectionate,Indoor", 125),
    ("milo", "Milo", "Dog", "Labrador Mix", 3, "Male", "Medium", "milo", 41.873, -87.659,
     "Chicago, IL", "One Tail at a Time", "An adventure buddy with a heart of gold. Milo is happiest exploring a trail and winding down with his favorite people.", "Active,Friendly,House trained", 200),
    ("willow", "Willow", "Rabbit", "Holland Lop", 1, "Female", "Small", "willow", 41.951, -87.678,
     "Chicago, IL", "Red Door Animal Shelter", "Small paws, big personality. Willow enjoys fresh greens, cozy hideaways, and gentle company.", "Gentle,Quiet,Curious", 80),
]

@contextmanager
def database():
    connection = sqlite3.connect(DB_PATH, timeout=10)
    try:
        connection.row_factory = sqlite3.Row
        connection.execute("PRAGMA foreign_keys = ON")
        with connection:
            yield connection
    finally:
        connection.close()

def initialize():
    DB_PATH.parent.mkdir(parents=True, exist_ok=True)
    with database() as db:
        db.executescript("""
        CREATE TABLE IF NOT EXISTS sessions (id INTEGER PRIMARY KEY, token_hash TEXT UNIQUE NOT NULL);
        CREATE TABLE IF NOT EXISTS pets (
            id TEXT PRIMARY KEY, name TEXT, species TEXT, breed TEXT, age INTEGER, sex TEXT,
            size TEXT, image_key TEXT, latitude REAL, longitude REAL, city TEXT, shelter TEXT,
            description TEXT, traits TEXT, fee INTEGER, available INTEGER NOT NULL DEFAULT 1);
        CREATE TABLE IF NOT EXISTS decisions (
            session_id INTEGER REFERENCES sessions(id), pet_id TEXT REFERENCES pets(id),
            decision TEXT CHECK(decision IN ('like','pass')), PRIMARY KEY(session_id, pet_id));
        CREATE TABLE IF NOT EXISTS applications (
            id INTEGER PRIMARY KEY, session_id INTEGER REFERENCES sessions(id),
            pet_id TEXT REFERENCES pets(id), name TEXT, email TEXT, message TEXT,
            status TEXT NOT NULL DEFAULT 'submitted', created_at TEXT,
            UNIQUE(session_id, pet_id));
        """)
        db.executemany("INSERT OR IGNORE INTO pets VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,1)", PETS)

@asynccontextmanager
async def lifespan(app):
    initialize()
    yield

app = FastAPI(title="Petinder API", version="1.0.0", lifespan=lifespan,
              description="Pet discovery, distance filtering, saved pets, and adoption applications. Seed listings are demonstrations.")

def current_session(credentials: Annotated[HTTPAuthorizationCredentials | None, Depends(security)]):
    if credentials is None:
        raise HTTPException(401, "Session required", headers={"WWW-Authenticate": "Bearer"})
    hashed = hashlib.sha256(credentials.credentials.encode()).hexdigest()
    with database() as db:
        row = db.execute("SELECT id FROM sessions WHERE token_hash=?", (hashed,)).fetchone()
    if row is None:
        raise HTTPException(401, "Invalid session", headers={"WWW-Authenticate": "Bearer"})
    return row["id"]

Session = Annotated[int, Depends(current_session)]

def distance_km(lat1, lon1, lat2, lon2):
    a, b = math.radians(lat1), math.radians(lat2)
    dlat, dlon = b - a, math.radians(lon2 - lon1)
    value = math.sin(dlat / 2) ** 2 + math.cos(a) * math.cos(b) * math.sin(dlon / 2) ** 2
    return 6371.0088 * 2 * math.asin(min(1, math.sqrt(value)))

def serialize_pet(row, lat=None, lon=None):
    pet = dict(row)
    pet["traits"] = pet["traits"].split(",")
    pet["available"] = bool(pet["available"])
    pet["distance_km"] = round(distance_km(lat, lon, pet["latitude"], pet["longitude"]), 2) if lat is not None else None
    return pet

def require_pet(db, pet_id):
    row = db.execute("SELECT * FROM pets WHERE id=?", (pet_id,)).fetchone()
    if row is None:
        raise HTTPException(404, "Pet not found")
    return row

@app.get("/health")
def health():
    return {"status": "ok", "catalog": "demo"}

@app.post("/sessions", status_code=201)
def create_session():
    token = secrets.token_urlsafe(32)
    with database() as db:
        db.execute("INSERT INTO sessions(token_hash) VALUES (?)", (hashlib.sha256(token.encode()).hexdigest(),))
    return {"token": token}

@app.get("/pets")
def list_pets(lat: float | None = Query(None, ge=-90, le=90),
              lon: float | None = Query(None, ge=-180, le=180),
              radius_km: float = Query(50, ge=1, le=500),
              species: Literal["Dog", "Cat", "Rabbit"] | None = None):
    if (lat is None) != (lon is None):
        raise HTTPException(422, "Latitude and longitude must be supplied together")
    with database() as db:
        rows = db.execute("SELECT * FROM pets WHERE available=1 ORDER BY rowid").fetchall()
    pets = [serialize_pet(row, lat, lon) for row in rows if species is None or row["species"] == species]
    if lat is not None:
        pets = [pet for pet in pets if distance_km(lat, lon, pet["latitude"], pet["longitude"]) <= radius_km]
        pets.sort(key=lambda pet: pet["distance_km"])
    return pets

@app.get("/pets/{pet_id}")
def get_pet(pet_id: str):
    with database() as db:
        return serialize_pet(require_pet(db, pet_id))

class Decision(BaseModel):
    decision: Literal["like", "pass"]

@app.put("/me/decisions/{pet_id}")
def decide(pet_id: str, body: Decision, session: Session):
    with database() as db:
        require_pet(db, pet_id)
        db.execute("INSERT INTO decisions VALUES (?,?,?) ON CONFLICT(session_id,pet_id) DO UPDATE SET decision=excluded.decision",
                   (session, pet_id, body.decision))
    return {"pet_id": pet_id, "decision": body.decision}

@app.get("/me/decisions")
def decisions(session: Session):
    with database() as db:
        return [dict(row) for row in db.execute("SELECT pet_id,decision FROM decisions WHERE session_id=?", (session,))]

@app.delete("/me/decisions/{pet_id}", status_code=204)
def undo_decision(pet_id: str, session: Session):
    with database() as db:
        db.execute("DELETE FROM decisions WHERE session_id=? AND pet_id=?", (session, pet_id))

@app.get("/me/favorites")
def favorites(session: Session):
    with database() as db:
        return [serialize_pet(row) for row in db.execute("SELECT p.* FROM pets p JOIN decisions d ON p.id=d.pet_id WHERE d.session_id=? AND d.decision='like'", (session,))]

class ApplicationInput(BaseModel):
    pet_id: str = Field(min_length=1, max_length=80)
    name: str = Field(min_length=2, max_length=100)
    email: str = Field(min_length=5, max_length=254, pattern=r"^[^\s@]+@[^\s@]+\.[^\s@]+$")
    message: str = Field(min_length=20, max_length=2000)

    @field_validator("name", "email", "message", mode="before")
    @classmethod
    def strip_text(cls, value):
        return value.strip() if isinstance(value, str) else value

@app.post("/me/applications", status_code=201)
def apply(body: ApplicationInput, session: Session):
    with database() as db:
        pet = require_pet(db, body.pet_id)
        if not pet["available"]:
            raise HTTPException(409, "This pet is no longer available")
        try:
            cursor = db.execute("INSERT INTO applications(session_id,pet_id,name,email,message,created_at) VALUES (?,?,?,?,?,?)",
                                (session, body.pet_id, body.name, body.email, body.message, datetime.now(timezone.utc).isoformat()))
        except sqlite3.IntegrityError:
            raise HTTPException(409, "You have already applied for this pet")
        return dict(db.execute("SELECT id,pet_id,status,created_at FROM applications WHERE id=?", (cursor.lastrowid,)).fetchone())

@app.get("/me/applications")
def applications(session: Session):
    with database() as db:
        return [dict(row) for row in db.execute("SELECT a.id,a.pet_id,p.name AS pet_name,a.status,a.created_at FROM applications a JOIN pets p ON p.id=a.pet_id WHERE session_id=? ORDER BY a.id DESC", (session,))]
