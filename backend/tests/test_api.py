"""Run from the repository root: python -m unittest discover -s backend/tests -v."""
from pathlib import Path
import tempfile
import unittest

from fastapi.testclient import TestClient
from backend import main


class ApiTests(unittest.TestCase):
    def setUp(self):
        self.temp = tempfile.TemporaryDirectory()
        self.original_path = main.DB_PATH
        main.DB_PATH = Path(self.temp.name) / "test.db"
        self.client = TestClient(main.app)
        self.client.__enter__()
        token = self.client.post("/sessions").json()["token"]
        self.headers = {"Authorization": f"Bearer {token}"}

    def tearDown(self):
        self.client.__exit__(None, None, None)
        main.DB_PATH = self.original_path
        self.temp.cleanup()

    def test_nearby_sorted_and_filtered(self):
        result = self.client.get("/pets", params={"lat": 41.8781, "lon": -87.6298, "radius_km": 3})
        self.assertEqual(result.status_code, 200)
        pets = result.json()
        self.assertGreater(len(pets), 0)
        distances = [p["distance_km"] for p in pets]
        self.assertEqual(distances, sorted(distances))
        self.assertTrue(all(d <= 3 for d in distances))
        cats = self.client.get("/pets", params={"species": "Cat"}).json()
        self.assertEqual([p["name"] for p in cats], ["Luna"])
        self.assertEqual(self.client.get("/pets", params={"lat": 0, "lon": 0, "radius_km": 1}).json(), [])

    def test_coordinate_validation(self):
        for params in ({"lat": 30}, {"lat": 91, "lon": 0}, {"radius_km": 0}, {"species": "invalid"}):
            with self.subTest(params=params):
                self.assertEqual(self.client.get("/pets", params=params).status_code, 422)

    def test_session_isolation_and_undo(self):
        path = "/me/decisions/buddy"
        self.assertEqual(self.client.put(path, json={"decision": "like"}).status_code, 401)
        self.assertEqual(self.client.get("/me/applications", headers={"Authorization": "Bearer invalid"}).status_code, 401)
        self.assertEqual(self.client.put(path, headers=self.headers, json={"decision": "like"}).status_code, 200)
        favorites = self.client.get("/me/favorites", headers=self.headers).json()
        self.assertEqual([p["id"] for p in favorites], ["buddy"])
        token = self.client.post("/sessions").json()["token"]
        self.assertEqual(self.client.get("/me/favorites", headers={"Authorization": f"Bearer {token}"}).json(), [])
        self.assertEqual(self.client.delete(path, headers=self.headers).status_code, 204)
        self.assertEqual(self.client.get("/me/favorites", headers=self.headers).json(), [])
        self.assertEqual(self.client.get("/me/decisions", headers=self.headers).json(), [])

    def test_application_validation_duplicates_and_persistence(self):
        body = {"pet_id": "buddy", "name": "Alex", "email": "alex@example.com", "message": "I have a quiet home and experience caring for dogs."}
        invalid = dict(body, email="bad email", message="short")
        self.assertEqual(self.client.post("/me/applications", headers=self.headers, json=invalid).status_code, 422)
        result = self.client.post("/me/applications", headers=self.headers, json=body)
        self.assertEqual(result.status_code, 201)
        self.assertEqual(result.json()["status"], "submitted")
        self.assertEqual(self.client.post("/me/applications", headers=self.headers, json=body).status_code, 409)
        with TestClient(main.app) as restarted:
            applications = restarted.get("/me/applications", headers=self.headers).json()
            self.assertEqual(len(applications), 1)
            self.assertEqual(applications[0]["pet_name"], "Buddy")
        with main.database() as db:
            db.execute("UPDATE pets SET available=0 WHERE id='luna'")
        self.assertEqual(self.client.post("/me/applications", headers=self.headers, json=dict(body, pet_id="luna")).status_code, 409)
        self.assertEqual(self.client.post("/me/applications", headers=self.headers, json=dict(body, pet_id="missing")).status_code, 404)

    def test_unknown_pet_and_invalid_decision(self):
        self.assertEqual(self.client.get("/pets/missing").status_code, 404)
        self.assertEqual(self.client.put("/me/decisions/missing", headers=self.headers, json={"decision": "like"}).status_code, 404)
        self.assertEqual(self.client.put("/me/decisions/buddy", headers=self.headers, json={"decision": "invalid"}).status_code, 422)

    def test_distance_calculation(self):
        self.assertAlmostEqual(main.distance_km(0, 0, 0, 0), 0)
        self.assertAlmostEqual(main.distance_km(0, 0, 0, 1), 111.195, places=2)


if __name__ == "__main__":
    unittest.main()
