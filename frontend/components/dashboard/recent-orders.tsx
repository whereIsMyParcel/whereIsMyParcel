"use client"

import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card"
import { Badge } from "@/components/ui/badge"
import { cn } from "@/lib/utils"

const orders = [
  {
    id: "ORD-001",
    product: "마른오징어 가공품",
    from: "경기 남부 센터",
    to: "부산광역시 센터",
    status: "배송중",
    quantity: 50,
  },
  {
    id: "ORD-002",
    product: "플라스틱 가공품",
    from: "서울특별시 센터",
    to: "대구광역시 센터",
    status: "준비중",
    quantity: 120,
  },
  {
    id: "ORD-003",
    product: "전자부품 세트",
    from: "인천광역시 센터",
    to: "광주광역시 센터",
    status: "완료",
    quantity: 80,
  },
  {
    id: "ORD-004",
    product: "식품 원재료",
    from: "대전광역시 센터",
    to: "울산광역시 센터",
    status: "배송중",
    quantity: 200,
  },
  {
    id: "ORD-005",
    product: "의류 원단",
    from: "경기 북부 센터",
    to: "경상남도 센터",
    status: "준비중",
    quantity: 150,
  },
]

const statusStyles = {
  배송중: "bg-info/20 text-info border-info/30",
  준비중: "bg-warning/20 text-warning border-warning/30",
  완료: "bg-success/20 text-success border-success/30",
}

export function RecentOrders() {
  return (
    <Card className="bg-card border-border">
      <CardHeader className="pb-3">
        <CardTitle className="text-lg font-semibold">최근 주문</CardTitle>
      </CardHeader>
      <CardContent>
        <div className="space-y-4">
          {orders.map((order) => (
            <div
              key={order.id}
              className="flex items-center justify-between p-3 rounded-lg bg-muted/50 hover:bg-muted transition-colors"
            >
              <div className="flex-1 min-w-0">
                <div className="flex items-center gap-2 mb-1">
                  <span className="text-sm font-medium text-primary">{order.id}</span>
                  <Badge
                    variant="outline"
                    className={cn("text-xs", statusStyles[order.status as keyof typeof statusStyles])}
                  >
                    {order.status}
                  </Badge>
                </div>
                <p className="text-sm font-medium truncate">{order.product}</p>
                <p className="text-xs text-muted-foreground">
                  {order.from} → {order.to}
                </p>
              </div>
              <div className="text-right">
                <p className="text-sm font-semibold">{order.quantity}개</p>
              </div>
            </div>
          ))}
        </div>
      </CardContent>
    </Card>
  )
}
