"use client"

import { DashboardLayout } from "@/components/layout/dashboard-layout"
import { Button } from "@/components/ui/button"
import { Input } from "@/components/ui/input"
import { Textarea } from "@/components/ui/textarea"
import { Badge } from "@/components/ui/badge"
import { Card, CardContent, CardHeader, CardTitle, CardDescription } from "@/components/ui/card"
import { MessageSquare, CheckCircle, XCircle, Clock, RefreshCw, Info } from "lucide-react"
import { useState, useEffect, useCallback } from "react"
import { cn } from "@/lib/utils"
import { fetchApi } from "@/lib/api"
import { toast } from "sonner"

// SlackStatus: READY_TO_SEND, MESSAGE_SENT, MESSAGE_FAILED, PERMANENT_FAILED
const statusConfig: Record<string, { label: string; icon: any; className: string }> = {
  READY_TO_SEND: { label: "발송 대기", icon: Clock, className: "bg-warning/20 text-warning border-warning/30" },
  MESSAGE_SENT: { label: "전송됨", icon: CheckCircle, className: "bg-success/20 text-success border-success/30" },
  MESSAGE_FAILED: { label: "실패", icon: XCircle, className: "bg-destructive/20 text-destructive border-destructive/30" },
  PERMANENT_FAILED: { label: "영구실패", icon: XCircle, className: "bg-destructive/20 text-destructive border-destructive/30" },
  // 하위 호환
  sent: { label: "전송됨", icon: CheckCircle, className: "bg-success/20 text-success border-success/30" },
  failed: { label: "실패", icon: XCircle, className: "bg-destructive/20 text-destructive border-destructive/30" },
  pending: { label: "대기중", icon: Clock, className: "bg-warning/20 text-warning border-warning/30" },
}

export default function SlackPage() {
  const [messages, setMessages] = useState<any[]>([])
  const [loading, setLoading] = useState(true)

  const loadMessages = useCallback(async () => {
    try {
      setLoading(true)
      const pageData = await fetchApi<any>('/ai-slack?size=50')
      setMessages(pageData.content || [])
    } catch (error) {
      console.error("Failed to fetch messages:", error)
    } finally {
      setLoading(false)
    }
  }, [])

  useEffect(() => {
    loadMessages()
  }, [loadMessages])

  const sentCount = messages.filter((m) => m.slackStatus === "MESSAGE_SENT" || m.status === "sent").length
  const failedCount = messages.filter((m) =>
    m.slackStatus === "MESSAGE_FAILED" || m.slackStatus === "PERMANENT_FAILED" || m.status === "failed"
  ).length
  const pendingCount = messages.filter((m) => m.slackStatus === "READY_TO_SEND" || m.status === "pending").length

  const formatDate = (dateStr: string) => {
    if (!dateStr) return "-"
    return new Date(dateStr).toLocaleString("ko-KR", {
      month: "2-digit", day: "2-digit",
      hour: "2-digit", minute: "2-digit"
    })
  }

  const getStatus = (message: any) => {
    return message.slackStatus || message.status || "pending"
  }

  return (
    <DashboardLayout>
      <div className="space-y-6">
        <div>
          <h1 className="text-2xl font-bold tracking-tight">슬랙 메시지</h1>
          <p className="text-muted-foreground">주문 생성 시 AI가 자동으로 발송한 슬랙 알림 내역입니다.</p>
        </div>

        <div className="grid gap-4 md:grid-cols-4">
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
              <div className="text-sm text-muted-foreground">대기중</div>
              <div className="text-3xl font-bold mt-1 text-warning">{pendingCount}</div>
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
          {/* 안내 카드 */}
          <Card className="bg-card border-border lg:col-span-1">
            <CardHeader>
              <CardTitle className="text-lg flex items-center gap-2">
                <Info className="w-5 h-5 text-info" />
                슬랙 발송 안내
              </CardTitle>
            </CardHeader>
            <CardContent className="space-y-4">
              <div className="rounded-lg bg-muted/50 p-4 space-y-3">
                <p className="text-sm text-muted-foreground">
                  슬랙 메시지는 <strong className="text-foreground">주문 생성 시</strong> AI가 배송 예상 시간을 분석한 후 자동으로 발송됩니다.
                </p>
                <p className="text-sm text-muted-foreground">
                  발송 대상은 <strong className="text-foreground">발송 허브 담당자</strong>이며, AI가 최종 발송 시한을 포함한 메시지를 생성합니다.
                </p>
              </div>
              <div className="space-y-2">
                <p className="text-xs font-medium text-muted-foreground">메시지 포함 내용</p>
                <ul className="text-xs text-muted-foreground space-y-1 list-disc list-inside">
                  <li>주문 번호 및 주문자 정보</li>
                  <li>상품 정보 및 수량</li>
                  <li>발송지 / 경유지 / 도착지</li>
                  <li>배송 담당자 정보</li>
                  <li>AI 분석 최종 발송 시한</li>
                </ul>
              </div>
              <Button
                variant="outline"
                className="w-full"
                onClick={loadMessages}
                disabled={loading}
              >
                <RefreshCw className={cn("w-4 h-4 mr-2", loading && "animate-spin")} />
                새로고침
              </Button>
            </CardContent>
          </Card>

          {/* 메시지 목록 */}
          <Card className="bg-card border-border lg:col-span-2">
            <CardHeader>
              <CardTitle className="text-lg">최근 발송 메시지</CardTitle>
              <CardDescription>주문 생성 시 자동 발송된 AI 슬랙 알림 내역</CardDescription>
            </CardHeader>
            <CardContent>
              <div className="space-y-3 max-h-[500px] overflow-y-auto pr-1">
                {loading ? (
                  <div className="text-center py-8 text-muted-foreground">
                    데이터를 불러오는 중입니다...
                  </div>
                ) : messages.length === 0 ? (
                  <div className="text-center py-12 text-muted-foreground">
                    <MessageSquare className="w-12 h-12 mx-auto mb-3 opacity-30" />
                    <p>발송된 메시지가 없습니다.</p>
                    <p className="text-xs mt-1">주문을 생성하면 슬랙 알림이 발송됩니다.</p>
                  </div>
                ) : (
                  messages.map((message, index) => {
                    const statusKey = getStatus(message)
                    const config = statusConfig[statusKey] || statusConfig.pending
                    const StatusIcon = config.icon
                    const recipient = message.slackId || message.receiverId || message.recipient || "-"
                    const content = message.message || message.content || "-"
                    const sentAt = message.sentAt || message.createdAt

                    return (
                      <div
                        key={message.id || `msg-${index}`}
                        className="flex items-start gap-4 p-4 rounded-lg bg-muted/50 hover:bg-muted transition-colors"
                      >
                        <div className="p-2 rounded-lg bg-primary/10 shrink-0">
                          <MessageSquare className="w-4 h-4 text-primary" />
                        </div>
                        <div className="flex-1 min-w-0">
                          <div className="flex items-center gap-2 mb-1 flex-wrap">
                            <span className="font-mono text-sm text-primary">{recipient}</span>
                            <Badge variant="outline" className={cn("text-xs", config.className)}>
                              <StatusIcon className="w-3 h-3 mr-1" />
                              {config.label}
                            </Badge>
                            {message.retryCount > 0 && (
                              <span className="text-xs text-muted-foreground">재시도 {message.retryCount}회</span>
                            )}
                          </div>
                          <p className="text-sm text-foreground whitespace-pre-wrap line-clamp-3">{content}</p>
                          <p className="text-xs text-muted-foreground mt-1">{formatDate(sentAt)}</p>
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
