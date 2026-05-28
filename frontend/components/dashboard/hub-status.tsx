"use client"

import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card"
import { Badge } from "@/components/ui/badge"
import { MapPin } from "lucide-react"
import { useState, useEffect } from "react"
import { fetchApi } from "@/lib/api"

export function HubStatus() {
  const [hubs, setHubs] = useState<any[]>([])

  useEffect(() => {
    fetchApi<any>('/hubs?size=30')
      .then((data) => setHubs(data.content || []))
      .catch(console.error)
  }, [])

  return (
    <Card className="bg-card border-border">
      <CardHeader className="pb-3">
        <CardTitle className="text-lg font-semibold">허브 현황</CardTitle>
      </CardHeader>
      <CardContent>
        <div className="grid grid-cols-1 md:grid-cols-2 gap-3">
          {hubs.length === 0 ? (
            <p className="text-sm text-muted-foreground col-span-2 text-center py-4">허브 데이터 없음</p>
          ) : (
            hubs.map((hub) => (
              <div
                key={hub.hubId}
                className="flex items-center gap-4 p-3 rounded-lg bg-muted/50 hover:bg-muted transition-colors"
              >
                <div className="p-2 rounded-lg bg-primary/10">
                  <MapPin className="w-4 h-4 text-primary" />
                </div>
                <div className="flex-1 min-w-0">
                  <div className="flex items-center gap-2">
                    <p className="text-sm font-medium truncate">{hub.name}</p>
                    <Badge variant="outline" className="text-xs bg-success/20 text-success border-success/30">
                      운영중
                    </Badge>
                  </div>
                  <p className="text-xs text-muted-foreground truncate">{hub.address}</p>
                </div>
              </div>
            ))
          )}
        </div>
      </CardContent>
    </Card>
  )
}
