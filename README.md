# Tiny Ledger

A small Spring Boot application that records money movements (deposits and withdrawals)
in a single in-memory ledger and exposes the current balance and transaction history over REST.

Tech stack: Java 21, Spring Boot 4.1, Maven, Lombok, Jakarta Bean Validation, JUnit 5, AssertJ, MockMvc.

## How to Run

Requires JDK 21 or newer. No other infrastructure is needed.

```bash
./mvnw spring-boot:run
```

On Windows use `mvnw.cmd spring-boot:run`.

The application listens on `http://localhost:8080`.

## How to Run Tests

```bash
./mvnw test
```

- `TransactionServiceTest` and `BalanceServiceTest` are unit tests for the business rules.
- `LedgerApiTest` exercises the REST API end to end with MockMvc, including invalid requests.

## API

All endpoints are under `/api/v1`. Request and response bodies are JSON.

### POST /api/v1/transactions

Records a deposit or a withdrawal.

| Field    | Type                        | Rules              |
|----------|-----------------------------|--------------------|
| `type`   | `DEPOSIT` or `WITHDRAWAL`   | required           |
| `amount` | decimal number              | required, `> 0`    |

The amount is always positive. The direction is given by `type`.

```bash
curl -i -X POST http://localhost:8080/api/v1/transactions \
  -H "Content-Type: application/json" \
  -d '{"type": "DEPOSIT", "amount": 100.00}'
```

`201 Created`

```json
{
  "id": "9a88a9c9-245c-4265-9a23-1eb406c7f03b",
  "type": "DEPOSIT",
  "amount": 100.00,
  "createdAt": "2026-09-24T11:32:52.971220100Z"
}
```

```bash
curl -i -X POST http://localhost:8080/api/v1/transactions \
  -H "Content-Type: application/json" \
  -d '{"type": "WITHDRAWAL", "amount": 30.50}'
```

`201 Created`

```json
{
  "id": "7a517989-5e62-4ea4-b185-20a49a811b0d",
  "type": "WITHDRAWAL",
  "amount": 30.50,
  "createdAt": "2026-09-24T11:32:53.021772600Z"
}
```

### GET /api/v1/transactions

Returns the full transaction history, oldest first.

```bash
curl http://localhost:8080/api/v1/transactions
```

`200 OK`

```json
[
  {
    "id": "9a88a9c9-245c-4265-9a23-1eb406c7f03b",
    "type": "DEPOSIT",
    "amount": 100.00,
    "createdAt": "2026-09-24T11:32:52.971220100Z"
  },
  {
    "id": "7a517989-5e62-4ea4-b185-20a49a811b0d",
    "type": "WITHDRAWAL",
    "amount": 30.50,
    "createdAt": "2026-09-24T11:32:53.021772600Z"
  }
]
```

### GET /api/v1/balance

Returns the current balance.

```bash
curl http://localhost:8080/api/v1/balance
```

`200 OK`

```json
{
  "balance": 69.50
}
```

### Errors

All errors have the same shape:

```json
{
  "code": "VALIDATION_ERROR",
  "message": "amount: must be greater than 0",
  "timestamp": "2026-09-24T11:32:53.212295900Z"
}
```

| Status                     | `code`               | When                                                              |
|----------------------------|----------------------|-------------------------------------------------------------------|
| `400 Bad Request`          | `VALIDATION_ERROR`   | missing `type` or `amount`, amount `<= 0`, unknown type, malformed JSON |
| `422 Unprocessable Content`| `INSUFFICIENT_FUNDS` | a withdrawal is larger than the current balance                    |

422 is called "Unprocessable Content" in RFC 9110 and in Spring; older sources call it "Unprocessable Entity".

```bash
curl -i -X POST http://localhost:8080/api/v1/transactions \
  -H "Content-Type: application/json" \
  -d '{"type": "WITHDRAWAL", "amount": 1000}'
```

`422 Unprocessable Content`

```json
{
  "code": "INSUFFICIENT_FUNDS",
  "message": "Insufficient funds: requested 1000, available 69.50",
  "timestamp": "2026-09-24T11:32:53.153193700Z"
}
```

A rejected request is not recorded and does not change the balance.

## Assumptions

### Single Ledger

The application represents a single ledger.

The requirements do not define accounts or multiple ledgers, so accounts and multiple ledgers
are considered outside the scope of this implementation.

### In-Memory State

All transactions are stored in memory.

Application state is lost when the process is restarted.

### Balance Cannot Go Negative

A withdrawal larger than the current balance is rejected with `422`. Withdrawing exactly the
current balance is allowed and leaves the balance at zero.

### Single Currency, Amounts As Given

There is one implicit currency. Amounts are stored and returned exactly as they were sent
(`100`, `100.00` and `100.000` are all accepted). The number of decimal places is not restricted.

### No Concurrency Control

The assignment states that atomic operations are not expected, so none are implemented, and the
application assumes requests are not processed concurrently. Specifically:

- the repository is a plain `ArrayList`, which is not thread-safe;
- a withdrawal reads the balance and saves the transaction in two separate steps, so two concurrent
  withdrawals could both pass the check and overdraw the ledger.

For this single-process app the simplest fix would be to serialize writes (one lock around
check-and-save); with a database, a transaction with appropriate locking.

## Design Decisions and Trade-offs

**Packaging.** Code is grouped by feature (`transaction`, `balance`) and then by technical role
(`controller`, `service`, `repository`, `dto`, `model`). Shared error handling lives in `common.exception`.
There are no extra architectural layers; this is a plain Spring Boot controller/service/repository app.

**In-memory storage.** The assignment asks for no external infrastructure, so `TransactionRepository`
keeps transactions in a `List` in insertion order. Insertion order is also chronological order,
which is why the history needs no sorting.

**`BigDecimal` for money.** `double` and `float` are binary floating-point types and cannot represent
values such as `0.10` exactly, so sums drift. `BigDecimal` keeps exact decimal values.

**Immutable records.** `Transaction`, the request/response DTOs and `ApiError` are records. A transaction
is a fact that never changes after it is recorded, and records make that explicit with little code.
The domain model is separate from the API DTOs so the API contract can change without touching the model.

**Balance is derived from history.** The balance is not stored. `BalanceService` sums deposits and
subtracts withdrawals on every request. This gives a single source of truth, so the balance can never
disagree with the history. The cost is O(n) per balance read, which is fine at this scale.

**Direction is the type, not the sign.** Amounts must be positive; `WITHDRAWAL 50.00` rather than
`-50.00`. This makes invalid combinations such as `DEPOSIT -50` impossible.

**Limitations of in-memory storage.**
- All data is lost on restart.
- Everything is held in heap memory, so the history cannot grow without bound.
- It does not work with more than one application instance.
- It is not safe for concurrent requests 
