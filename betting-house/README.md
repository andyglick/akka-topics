grpcurl -d  '{ 
  "marketId": "1243",
  "fixture": {"id": "id1", "homeTeam": "RM", "awayTeam": "MU"},
  "odds": {"winHome": 1.25, "winAway": 1.70, "tie": 1.10 },
  "opensAt": 123
}' -plaintext localhost:9000 MarketService/Initialize


grpcurl -d '{"marketId": "1243"}' -plaintext localhost:9000 MarketService/GetState



curl "localhost:9001/wallet/add?walletId=123"


curl -XPOST "localhost:9001/wallet/add?walletId=123&funds=222"


curl -XPOST "localhost:9001/wallet/remove?walletId=123&funds=333"






172.17.0.6:9000


curl -L https://github.com/kubernetes/kompose/releases/download/v1.24.0/kompose-darwin-amd64 -o kompose


