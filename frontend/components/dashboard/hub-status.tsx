"use client"

import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card"
import { Badge } from "@/components/ui/badge"
import { cn } from "@/lib/utils"
import { MapPin } from "lucide-react"

const hubs = [
  { name: "서울특별시 센터", address: "송파구 송파대로 55", status: "active", orders: 45 },
  { name: "경기 북부 센터", address: "고양시 덕양구 권율대로 570", status: "active", orders: 32 },
  { name: "경기 남부 센터", address: "이천시 덕평로 257-21", status: "active", orders: 58 },
  { name: "부산광역시 센터", address: "동구 중앙대로 206", status: "active", orders: 27 },
  { name: "대구광역시 센터", address: "북구 태평로 161", status: "maintenance", orders: 12 },
  { name: "인천광역시 센터", address: "남동구 정각로 29", status: "active", orders: 39 },
]

export function HubStatus() {
  return (
    <Card className="bg-card border-border">
      <CardHeader className="pb-3">
        <CardTitle className="text-lg font-semibold">허브 현황</CardTitle>
      </CardHeader>
      <CardContent>
        <div className="space-y-3">
          {hubs.map((hub) => (
            <div
              key={hub.name}
              className="flex items-center gap-4 p-3 rounded-lg bg-muted/50 hover:bg-muted transition-colors"
            >
              <div className="p-2 rounded-lg bg-primary/10">
                <MapPin className="w-4 h-4 text-primary" />
              </div>
              <div className="flex-1 min-w-0">
                <div className="flex items-center gap-2">
                  <p className="text-sm font-medium truncate">{hub.name}</p>
                  <Badge
                    variant="outline"
                    className={cn(
                      "text-xs",
                      hub.status === "active"
                        ? "bg-success/20 text-success border-success/30"
                        : "bg-warning/20 text-warning border-warning/30"
                    )}
                  >
                    {hub.status === "active" ? "운영중" : "점검중"}
                  </Badge>
                </div>
                <p className="text-xs text-muted-foreground truncate">{hub.address}</p>
              </div>
              <div className="text-right">
                <p className="text-sm font-semibold">{hub.orders}</p>
                <p className="text-xs text-muted-foreground">처리중</p>
              </div>
            </div>
          ))}
        </div>
      </CardContent>
    </Card>
  )
}
