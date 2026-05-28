"use client"

import { DashboardLayout } from "@/components/layout/dashboard-layout"
import { Button } from "@/components/ui/button"
import { Input } from "@/components/ui/input"
import {
  Table,
  TableBody,
  TableCell,
  TableHead,
  TableHeader,
  TableRow,
} from "@/components/ui/table"
import { Badge } from "@/components/ui/badge"
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card"
import {
  DropdownMenu,
  DropdownMenuContent,
  DropdownMenuItem,
  DropdownMenuTrigger,
} from "@/components/ui/dropdown-menu"
import { Plus, Search, MoreHorizontal, Eye, Trash2, XCircle } from "lucide-react"
import { useState, useEffect, useCallback } from "react"
import { cn } from "@/lib/utils"
import { fetchApi } from "@/lib/api"
import { CreateOrderModal } from "@/components/orders/create-order-modal"
import { toast } from "sonner"

// OrderStatus: PENDING, STOCK_RESERVED, CONFIRMED, CANCELLED, COMPLETED, FAILED
const statusConfig: Record<string, { label: string; className: string }> = {
  PENDING: { label: "대기중", className: "bg-warning/20 text-warning border-warning/30" },
  STOCK_RESERVED: { label: "재고예약", className: "bg-info/20 text-info border-info/30" },
  CONFIRMED: { label: "확정", className: "bg-primary/20 text-primary border-primary/30" },
  CANCELLED: { label: "취소", className: "bg-destructive/20 text-destructive border-destructive/30" },
  COMPLETED: { label: "완료", className: "bg-success/20 text-success border-success/30" },
  FAILED: { label: "실패", className: "bg-destructive/20 text-destructive border-destructive/30" },
}

export default function OrdersPage() {
  const [orders, setOrders] = useState<any[]>([])
  const [loading, setLoading] = useState(true)
  const [searchTerm, setSearchTerm] = useState("")
  const [statusFilter, setStatusFilter] = useState<string>("all")
  const [isCreateModalOpen, setIsCreateModalOpen] = useState(false)

  const loadOrders = useCallback(async () => {
    try {
      setLoading(true)
      const pageData = await fetchApi<any>('/orders?size=50')
      setOrders(pageData.content || [])
    } catch (error) {
      console.error("Failed to fetch orders:", error)
      toast.error("주문 목록을 불러오지 못했습니다.")
    } finally {
      setLoading(false)
    }
  }, [])

  useEffect(() => {
    loadOrders()
  }, [loadOrders])

  const handleCancelOrder = async (orderId: string) => {
    if (!confirm("정말 주문을 취소하시겠습니까?")) return
    try {
      await fetchApi(`/orders/${orderId}/cancel`, { method: 'PATCH' })
      toast.success("주문이 취소되었습니다.")
      loadOrders()
    } catch (error: any) {
      toast.error(error.message || "주문 취소에 실패했습니다.")
    }
  }

  const handleDeleteOrder = async (orderId: string) => {
    if (!confirm("정말 주문을 삭제하시겠습니까?")) return
    try {
      await fetchApi(`/orders/${orderId}`, { method: 'DELETE' })
      toast.success("주문이 삭제되었습니다.")
      loadOrders()
    } catch (error: any) {
      toast.error(error.message || "주문 삭제에 실패했습니다.")
    }
  }

  const filteredOrders = orders.filter((order) => {
    const orderNumber = order.orderNumber || ""
    const recipientName = order.recipientName || ""
    const matchesSearch =
      orderNumber.toLowerCase().includes(searchTerm.toLowerCase()) ||
      recipientName.toLowerCase().includes(searchTerm.toLowerCase())
    const matchesStatus = statusFilter === "all" || order.orderStatus === statusFilter
    return matchesSearch && matchesStatus
  })

  const statusCounts = {
    PENDING: orders.filter((o) => o.orderStatus === "PENDING").length,
    STOCK_RESERVED: orders.filter((o) => o.orderStatus === "STOCK_RESERVED").length,
    CONFIRMED: orders.filter((o) => o.orderStatus === "CONFIRMED").length,
    COMPLETED: orders.filter((o) => o.orderStatus === "COMPLETED").length,
  }

  const formatDate = (dateStr: string) => {
    if (!dateStr) return "-"
    return new Date(dateStr).toLocaleDateString("ko-KR", {
      year: "numeric", month: "2-digit", day: "2-digit",
      hour: "2-digit", minute: "2-digit"
    })
  }

  return (
    <DashboardLayout>
      <div className="space-y-6">
        <div className="flex items-center justify-between">
          <div>
            <h1 className="text-2xl font-bold tracking-tight">주문 관리</h1>
            <p className="text-muted-foreground">배송 주문을 생성하고 관리합니다.</p>
          </div>
          <Button className="bg-primary hover:bg-primary/90" onClick={() => setIsCreateModalOpen(true)}>
            <Plus className="w-4 h-4 mr-2" />
            주문 생성
          </Button>
        </div>

        <div className="grid gap-4 md:grid-cols-4">
          <Card className="bg-card border-border">
            <CardContent className="p-6">
              <div className="text-sm text-muted-foreground">전체 주문</div>
              <div className="text-3xl font-bold mt-1">{orders.length}</div>
            </CardContent>
          </Card>
          <Card className="bg-card border-border">
            <CardContent className="p-6">
              <div className="text-sm text-muted-foreground">대기중</div>
              <div className="text-3xl font-bold mt-1 text-warning">{statusCounts.PENDING}</div>
            </CardContent>
          </Card>
          <Card className="bg-card border-border">
            <CardContent className="p-6">
              <div className="text-sm text-muted-foreground">재고예약</div>
              <div className="text-3xl font-bold mt-1 text-info">{statusCounts.STOCK_RESERVED}</div>
            </CardContent>
          </Card>
          <Card className="bg-card border-border">
            <CardContent className="p-6">
              <div className="text-sm text-muted-foreground">완료</div>
              <div className="text-3xl font-bold mt-1 text-success">{statusCounts.COMPLETED}</div>
            </CardContent>
          </Card>
        </div>

        <Card className="bg-card border-border">
          <CardHeader className="pb-4">
            <div className="flex items-center justify-between">
              <div className="flex items-center gap-2">
                <CardTitle className="text-lg">주문 목록</CardTitle>
                <div className="flex gap-1 ml-4 flex-wrap">
                  {["all", "PENDING", "STOCK_RESERVED", "CONFIRMED", "COMPLETED", "CANCELLED"].map((status) => (
                    <Button
                      key={status}
                      variant={statusFilter === status ? "default" : "ghost"}
                      size="sm"
                      onClick={() => setStatusFilter(status)}
                      className={statusFilter === status ? "bg-primary" : ""}
                    >
                      {status === "all" ? "전체" : statusConfig[status]?.label || status}
                    </Button>
                  ))}
                </div>
              </div>
              <div className="relative w-72">
                <Search className="absolute left-3 top-1/2 -translate-y-1/2 w-4 h-4 text-muted-foreground" />
                <Input
                  placeholder="주문번호, 수령인 검색..."
                  className="pl-10 bg-muted border-0"
                  value={searchTerm}
                  onChange={(e) => setSearchTerm(e.target.value)}
                />
              </div>
            </div>
          </CardHeader>
          <CardContent>
            <Table>
              <TableHeader>
                <TableRow className="border-border hover:bg-transparent">
                  <TableHead className="text-muted-foreground">주문번호</TableHead>
                  <TableHead className="text-muted-foreground">수령인</TableHead>
                  <TableHead className="text-muted-foreground">총 금액</TableHead>
                  <TableHead className="text-muted-foreground">요청 납기일</TableHead>
                  <TableHead className="text-muted-foreground">주문일시</TableHead>
                  <TableHead className="text-muted-foreground">상태</TableHead>
                  <TableHead className="text-muted-foreground w-12"></TableHead>
                </TableRow>
              </TableHeader>
              <TableBody>
                {loading ? (
                  <TableRow>
                    <TableCell colSpan={7} className="text-center py-8 text-muted-foreground">
                      데이터를 불러오는 중입니다...
                    </TableCell>
                  </TableRow>
                ) : filteredOrders.length === 0 ? (
                  <TableRow>
                    <TableCell colSpan={7} className="text-center py-8 text-muted-foreground">
                      등록된 주문이 없습니다.
                    </TableCell>
                  </TableRow>
                ) : (
                  filteredOrders.map((order, index) => (
                    <TableRow key={order.orderId || `ord-${index}`} className="border-border hover:bg-muted/50">
                      <TableCell className="font-mono text-sm text-primary">{order.orderNumber || order.orderId?.slice(0, 8) + '...'}</TableCell>
                      <TableCell>
                        <div>
                          <p className="font-medium">{order.recipientName || "-"}</p>
                          <p className="text-xs text-muted-foreground">
                            {order.totalPrice?.toLocaleString()}원
                          </p>
                        </div>
                      </TableCell>
                      <TableCell className="font-mono">
                        {order.totalPrice?.toLocaleString() || 0}원
                      </TableCell>
                      <TableCell className="text-muted-foreground text-sm">
                        {formatDate(order.requestedDeliveryAt)}
                      </TableCell>
                      <TableCell className="text-muted-foreground text-sm">
                        {formatDate(order.orderedAt || order.createdAt)}
                      </TableCell>
                      <TableCell>
                        <Badge
                          variant="outline"
                          className={cn(statusConfig[order.orderStatus]?.className)}
                        >
                          {statusConfig[order.orderStatus]?.label || order.orderStatus}
                        </Badge>
                      </TableCell>
                      <TableCell>
                        <DropdownMenu>
                          <DropdownMenuTrigger asChild>
                            <Button variant="ghost" size="icon" className="h-8 w-8">
                              <MoreHorizontal className="w-4 h-4" />
                            </Button>
                          </DropdownMenuTrigger>
                          <DropdownMenuContent align="end">
                            <DropdownMenuItem>
                              <Eye className="w-4 h-4 mr-2" />
                              상세보기
                            </DropdownMenuItem>
                            {order.orderStatus !== "CANCELLED" && order.orderStatus !== "COMPLETED" && (
                              <DropdownMenuItem
                                className="text-warning cursor-pointer"
                                onClick={() => handleCancelOrder(order.orderId)}
                              >
                                <XCircle className="w-4 h-4 mr-2" />
                                취소
                              </DropdownMenuItem>
                            )}
                            <DropdownMenuItem
                              className="text-destructive cursor-pointer"
                              onClick={() => handleDeleteOrder(order.orderId)}
                            >
                              <Trash2 className="w-4 h-4 mr-2" />
                              삭제
                            </DropdownMenuItem>
                          </DropdownMenuContent>
                        </DropdownMenu>
                      </TableCell>
                    </TableRow>
                  ))
                )}
              </TableBody>
            </Table>
          </CardContent>
        </Card>
      </div>
      <CreateOrderModal
        open={isCreateModalOpen}
        onOpenChange={setIsCreateModalOpen}
        onSuccess={loadOrders}
      />
    </DashboardLayout>
  )
}
