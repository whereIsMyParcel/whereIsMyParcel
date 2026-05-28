"use client"

import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card"
import { Badge } from "@/components/ui/badge"
import { cn } from "@/lib/utils"
import { useState, useEffect } from "react"
import { fetchApi } from "@/lib/api"

const statusStyles: Record<string, string> = {
  PENDING: "bg-warning/20 text-warning border-warning/30",
  STOCK_RESERVED: "bg-info/20 text-info border-info/30",
  CONFIRMED: "bg-primary/20 text-primary border-primary/30",
  CANCELLED: "bg-destructive/20 text-destructive border-destructive/30",
  COMPLETED: "bg-success/20 text-success border-success/30",
  FAILED: "bg-destructive/20 text-destructive border-destructive/30",
}

const statusLabels: Record<string, string> = {
  PENDING: "대기중",
  STOCK_RESERVED: "재고예약",
  CONFIRMED: "확정",
  CANCELLED: "취소",
  COMPLETED: "완료",
  FAILED: "실패",
}

export function RecentOrders() {
  const [orders, setOrders] = useState<any[]>([])

  useEffect(() => {
    fetchApi<any>('/orders?size=5')
      .then((data) => setOrders(data.content || []))
      .catch(console.error)
  }, [])

  return (
    <Card className="bg-card border-border">
      <CardHeader className="pb-3">
        <CardTitle className="text-lg font-semibold">최근 주문</CardTitle>
      </CardHeader>
      <CardContent>
        <div className="space-y-3">
          {orders.length === 0 ? (
            <p className="text-sm text-muted-foreground text-center py-6">주문 데이터 없음</p>
          ) : (
            orders.map((order, i) => (
              <div
                key={order.orderId || i}
                className="flex items-center justify-between p-3 rounded-lg bg-muted/50 hover:bg-muted transition-colors"
              >
                <div className="flex-1 min-w-0">
                  <div className="flex items-center gap-2 mb-1">
                    <span className="text-sm font-mono text-primary">{order.orderNumber || order.orderId?.slice(0, 8) + '...'}</span>
                    <Badge
                      variant="outline"
                      className={cn("text-xs", statusStyles[order.orderStatus] || "")}
                    >
                      {statusLabels[order.orderStatus] || order.orderStatus}
                    </Badge>
                  </div>
                  <p className="text-sm font-medium truncate">{order.recipientName || "수령인 미지정"}</p>
                  <p className="text-xs text-muted-foreground">
                    {order.totalPrice?.toLocaleString()}원
                  </p>
                </div>
              </div>
            ))
          )}
        </div>
      </CardContent>
    </Card>
  )
}
