import requests
import time
import threading
import random
import uuid
import warnings
import logging
from urllib3.exceptions import NotOpenSSLWarning

# 1. SILENCE THE WARNINGS (Must be at the very top)
# this script will use random UUIDs to force a full scan of the table. Evaluating the worst scenario
# of performance of the hospital-core api.
warnings.filterwarnings("ignore", category=NotOpenSSLWarning)
logging.getLogger("urllib3").setLevel(logging.ERROR)

# --- CONFIGURATION ---
BASE_URL = "http://localhost:8080/api/v1"
KC_URL = "http://localhost:8180/auth/realms/hospital-realm/protocol/openid-connect/token"
CLIENT_ID = "hospital-app"
USERNAME = "testuser"
PASSWORD = "password123"

DURATION_SECONDS = 300
CONCURRENT_USERS = 7
# ---------------------

class LoadGenerator:
    def __init__(self):
        self.token = None
        self.stop_event = threading.Event()
        self.stats_lock = threading.Lock()
        self.stats = {"search_hits": 0, "appointment_hits": 0, "errors": 0}

    def get_token(self):
        payload = {
            'grant_type': 'password',
            'client_id': CLIENT_ID,
            'username': USERNAME,
            'password': PASSWORD
        }
        try:
            response = requests.post(KC_URL, data=payload, timeout=10)
            if response.status_code != 200:
                print(f"FAILED TO GET TOKEN: {response.text}")
                exit(1)
            self.token = response.json()['access_token']
            print("Successfully retrieved JWT Token.")
        except Exception as e:
            print(f"Connection Error to Keycloak: {e}")
            exit(1)

    def trigger_search(self):
        headers = {"Authorization": f"Bearer {self.token}"}
        test_email = f"{uuid.uuid4()}@hospital.com"
        try:
            resp = requests.get(f"{BASE_URL}/patients/search",
                                params={"email": test_email},
                                headers=headers,
                                timeout=5)
            if resp.status_code == 401:
                print("Token expired. Re-authenticating...")
                self.get_token()
                return self.trigger_search()
            with self.stats_lock:
                if resp.status_code in [200, 404]:
                    self.stats["search_hits"] += 1
                else:
                    if self.stats["errors"] == 0:
                        print(f"DEBUG: First Error Code: {resp.status_code} | Body: {resp.text}")
                    self.stats["errors"] += 1
        except Exception:
            with self.stats_lock:
                self.stats["errors"] += 1

    def trigger_appointments(self):
        headers = {"Authorization": f"Bearer {self.token}"}
        try:
            resp = requests.get(f"{BASE_URL}/appointments",
                                headers=headers,
                                timeout=10)
            with self.stats_lock:
                if resp.status_code == 200:
                    self.stats["appointment_hits"] += 1
                else:
                    self.stats["errors"] += 1
        except Exception:
            with self.stats_lock:
                self.stats["errors"] += 1

    def worker(self, user_id):
        print(f"User-{user_id} thread started...")
        while not self.stop_event.is_set():
            choice = random.random()
            if choice < 0.7:
                self.trigger_search()
            else:
                self.trigger_appointments()

            # Slow down slightly to avoid local CPU bottleneck
            time.sleep(0.05)
        print(f"User-{user_id} thread stopping.")

    def run(self):
        self.get_token()
        print(f"Starting load for {DURATION_SECONDS} seconds...")

        threads = []
        for i in range(CONCURRENT_USERS):
            t = threading.Thread(target=self.worker, args=(i,))
            t.start()
            threads.append(t)

        # Let it run for the duration
        try:
            time.sleep(DURATION_SECONDS)
        except KeyboardInterrupt:
            print("\nManual stop detected...")

        print("\nStopping threads...")
        self.stop_event.set()

        for t in threads:
            t.join()

        print("\n--- LOAD TEST COMPLETE ---")
        print(f"Search Requests (Missing Index Test): {self.stats['search_hits']}")
        print(f"Appointment Requests (N+1 Test):      {self.stats['appointment_hits']}")
        print(f"Total Errors:                          {self.stats['errors']}")

if __name__ == "__main__":
    LoadGenerator().run()