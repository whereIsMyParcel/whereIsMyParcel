"use client"

import { DashboardLayout } from "@/components/layout/dashboard-layout"
import { Button } from "@/components/ui/button"
import { Input } from "@/components/ui/input"
import { Textarea } from "@/components/ui/textarea"
import { Badge } from "@/components/ui/badge"
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card"
import { MessageSquare, Send, CheckCircle, XCircle, Clock } from "lucide-react"
import { useState, useEffect } from "react"
import { cn } from "@/lib/utils"
import { fetchApi } from "@/lib/api"



const statusConfig = {
  sent: { label: "전송됨", icon: CheckCircle, className: "text-success" },
  failed: { label: "실패", icon: XCircle, className: "text-destructive" },
  pending: { label: "대기중", icon: Clock, className: "text-warning" },
}

export default function SlackPage() {
  const [messages, setMessages] = useState<any[]>([])
  const [loading, setLoading] = useState(true)
  const [newMessage, setNewMessage] = useState("")
  const [recipient, setRecipient] = useState("")

  useEffect(() => {
    async function loadMessages() {
      try {
        setLoading(true)
        const pageData = await fetchApi<any>('/slack-messages?size=50')
        setMessages(pageData.content || [])
      } catch (error) {
        console.error("Failed to fetch messages:", error)
      } finally {
        setLoading(false)
      }
    }
    loadMessages()
  }, [])

  const sentCount = messages.filter((m) => m.status === "sent").length
  const failedCount = messages.filter((m) => m.status === "failed").length

  return (
    <DashboardLayout>
      <div className="space-y-6">
        <div>
          <h1 className="text-2xl font-bold tracking-tight">슬랙 메시지</h1>
          <p className="text-muted-foreground">슬랙을 통해 알림 메시지를 발송합니다.</p>
        </div>

        <div className="grid gap-4 md:grid-cols-3">
          <Card className="bg-card border-border">
            <CardContent className="p-6">
              <div className="text-sm text-muted-foreground">전체 메시지</div>
              <div className="text-3xl font-bold mt-1">{messages.length}</div>
            </CardContent>
          </Card>
          <Card className="bg-card border-border">
            <CardContent className="p-6">
              <div className="text-sm text-muted-foreground">전송 성공</div>
              <div className="text-3xl font-bold mt-1 text-success">{sentCount}</div>
            </CardContent>
          </Card>
          <Card className="bg-card border-border">
            <CardContent className="p-6">
              <div className="text-sm text-muted-foreground">전송 실패</div>
              <div className="text-3xl font-bold mt-1 text-destructive">{failedCount}</div>
            </CardContent>
          </Card>
        </div>

        <div className="grid gap-6 lg:grid-cols-3">
          <Card className="bg-card border-border lg:col-span-1">
            <CardHeader>
              <CardTitle className="text-lg">새 메시지 발송</CardTitle>
            </CardHeader>
            <CardContent className="space-y-4">
              <div>
                <label className="text-sm text-muted-foreground mb-2 block">수신자</label>
                <Input
                  placeholder="@username 또는 #channel"
                  className="bg-muted border-0"
                  value={recipient}
                  onChange={(e) => setRecipient(e.target.value)}
                />
              </div>
              <div>
                <label className="text-sm text-muted-foreground mb-2 block">메시지 내용</label>
                <Textarea
                  placeholder="메시지를 입력하세요..."
                  className="bg-muted border-0 min-h-32 resize-none"
                  value={newMessage}
                  onChange={(e) => setNewMessage(e.target.value)}
                />
              </div>
              <Button className="w-full bg-primary hover:bg-primary/90">
                <Send className="w-4 h-4 mr-2" />
                메시지 발송
              </Button>
            </CardContent>
          </Card>

          <Card className="bg-card border-border lg:col-span-2">
            <CardHeader>
              <CardTitle className="text-lg">최근 메시지</CardTitle>
            </CardHeader>
            <CardContent>
              <div className="space-y-3">
                {loading ? (
                  <div className="text-center py-8 text-muted-foreground">
                    데이터를 불러오는 중입니다...
                  </div>
                ) : messages.length === 0 ? (
                  <div className="text-center py-8 text-muted-foreground">
                    메시지가 없습니다.
                  </div>
                ) : (
                  messages.map((message, index) => {
                    const statusKey = message.status as keyof typeof statusConfig
                    const config = statusConfig[statusKey] || statusConfig.pending
                    const StatusIcon = config.icon
                    return (
                      <div
                        key={message.id || `msg-${index}`}
                        className="flex items-start gap-4 p-4 rounded-lg bg-muted/50 hover:bg-muted transition-colors"
                      >
                        <div className="p-2 rounded-lg bg-primary/10">
                          <MessageSquare className="w-4 h-4 text-primary" />
                        </div>
                        <div className="flex-1 min-w-0">
                          <div className="flex items-center gap-2 mb-1">
                            <span className="font-mono text-sm text-primary">{message.recipient}</span>
                            <Badge
                              variant="outline"
                              className={cn(
                                "text-xs",
                                message.status === "sent" && "bg-success/20 text-success border-success/30",
                                message.status === "failed" && "bg-destructive/20 text-destructive border-destructive/30",
                                message.status === "pending" && "bg-warning/20 text-warning border-warning/30"
                              )}
                            >
                              <StatusIcon className="w-3 h-3 mr-1" />
                              {config.label}
                            </Badge>
                          </div>
                          <p className="text-sm text-foreground">{message.content}</p>
                          <p className="text-xs text-muted-foreground mt-1">{message.sentAt}</p>
                        </div>
                      </div>
                    )
                  })
                )}
              </div>
            </CardContent>
          </Card>
        </div>
      </div>
    </DashboardLayout>
  )
}
