# SmartCart: Android e-commerce app with rule-based return fraud detection

College project (Java, XML, SQLite). No backend, paid APIs or real payment/shipping.
IMPORTANT: fraud scoring is a hand-written, rule-based PROTOTYPE. It is not a trained or proven fraud-detection model.

## Setup
1. Install Android Studio (Hedgehog or newer, JDK 17 is bundled).
2. File > Open > select the `SmartCart` folder. Let Gradle sync (it downloads Gradle 8.7 and the libraries; internet needed once).
3. Run on an emulator or phone (Android 7.0 / API 24 or higher).
4. To reset the demo data: uninstall the app or clear its storage.

## Demo accounts
| Role | Email | Password | Demonstrates |
|---|---|---|---|
| Admin | admin@smartcart.com | admin123 | Dashboard, decisions, rule settings |
| Alice (Silver) | alice@demo.com | demo123 | LOW risk (score 0) on order 4 |
| Bob (Silver) | bob@demo.com | demo123 | MEDIUM risk (25) on order 7 |
| Carl (Silver) | carl@demo.com | demo123 | HIGH risk (60) on order 11, dumbbells |
| Diana (Gold) | diana@demo.com | demo123 | Gold membership benefits and six qualifying delivered orders |
Tap the demo names on the login screen to autofill.

## Packages and main classes
- `db/DBHelper` all SQLite tables and queries; `db/SeedData` catalogue (20 products) and demo history.
- `model/` plain data classes (User, Product, Order, ReturnRequest, CustomerHistory ...) and `Constants`.
- `logic/AppConfig` every configurable number (return window, risk scoring, and membership score thresholds). Admin can edit them in the app (menu > Rules and settings).
- `logic/FraudDetectionManager` explainable risk score (6 rules, listed below).
- `logic/MembershipManager` Bronze/Silver/Gold calculation, benefits, progress.
- `logic/ReturnEligibilityManager` the return workflow plus admin actions and the pickup/refund simulation.
- `ui/` activities and fragments; `SimpleAdapter` is one reusable RecyclerView adapter.
- `util/` Session (SharedPreferences), Security (salted SHA-256), Util helpers.

## Fraud rules (defaults)
+25 high return rate (>= 40% of delivered units, needs >= 3 delivered orders); +25 two or more rejected requests in 90 days;
+20 same product returned before; +15 another active request on the same order; +15 three or more recent requests in 30 days;
+5 no previous completed purchase (informational). Low 0-24, Medium 25-49, High 50+.
Low = auto-approved, Medium = "Under Review" (additional verification), High = "Pending" flagged for admin. Nothing is auto-rejected.
Pending and Under Review requests are never counted as completed returns. Customers never see fraud-risk scores or explanations.

## Membership score
Each placed purchase order adds 10 membership points. The first return with a reason of "Other", "Changed my mind", or "Not as described" is not penalized; each later request repeating that same reason subtracts 10 points. Other return reasons do not subtract points. Scores start at 0 (Bronze); Silver starts at 30 and Gold at 60. Admins can change the point values and tier thresholds in Rules and settings. The admin return list and request detail show each customer's current tier and score.

## Viva walkthrough (shopping to return)
1. Register a new user (or use Alice). Welcome screen appears.
2. Home: search, filter by category chip, heart a product (Wishlist tab), open details.
3. Add to cart, change quantity (stock is validated), Checkout, enter address, place the COD order. Confirmation dialog shows the order ID.
4. Profile > My orders > open the order > "Demo: mark as Shipped", then "Delivered".
5. Tap "Return" on an item, choose quantity and reason, submit. New user = score 5 = Low, so it is auto-approved.
6. Profile > Return history: use "Demo: simulate pickup" then "simulate refund". Check Notifications and Membership.
7. Out-of-window test: in an order use "Demo: make delivery 8 days old", then try a return (rejected by policy).
8. Log in as Bob, return an item from the newest order (Medium, goes to Under Review). Log in as Carl and return the dumbbells from the newest order (High, Pending).
9. Log in as Admin: stats, filter, open a request, read the risk explanation and history, approve or reject (reason required), change status.

## Honest limitations
- Product "images" are emoji tiles (no image files or internet needed).
- Delivery, pickup and refund are local simulations driven by the demo buttons.
- "Approved" on the dashboard counts Approved, Pickup Scheduled and Refund Processed.
- Membership recalculates after purchases are placed, returns are submitted and admin decisions are made.
