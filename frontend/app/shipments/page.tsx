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
import {
  Dialog,
  DialogContent,
  DialogHeader,
  DialogTitle,
} from "@/components/ui/dialog"
import { Search, MoreHorizontal, Eye, PlayCircle, CheckCircle, Trash2, ArrowRight } from "lucide-react"
import { useState, useEffect, useCallback } from "react"
import { cn } from "@/lib/utils"
import { fetchApi } from "@/lib/api"
import { toast } from "sonner"

const shipmentStatusConfig: Record<string, { label: string; className: string }> = {
  HUB_WAITING: { label: "허브 대기중", className: "bg-muted text-muted-foreground border-muted" },
  HUB_MOVING: { label: "허브 이동중", className: "bg-info/20 text-info border-info/30" },
  DESTINATION_HUB_ARRIVED: { label: "도착 허브 도착", className: "bg-primary/20 text-primary border-primary/30" },
  DELIVERING: { label: "배송중", className: "bg-warning/20 text-warning border-warning/30" },
  COMPANY_MOVING: { label: "업체 이동중", className: "bg-warning/20 text-warning border-warning/30" },
  DELIVERED: { label: "배송완료", className: "bg-success/20 text-success border-success/30" },
  CANCELLED: { label: "취소", className: "bg-destructive/20 text-destructive border-destructive/30" },
}

export default function ShipmentsPage() {
  const [shipments, setShipments] = useState<any[]>([])
  const [loading, setLoading] = useState(true)
  const [searchTerm, setSearchTerm] = useState("")
  const [statusFilter, setStatusFilter] = useState("all")
  const [selectedShipment, setSelectedShipment] = useState<any>(null)
  const [detailOpen, setDetailOpen] = useState(false)

  const loadShipments = useCallback(async () => {
    try {
      setLoading(true)
      const pageData = await fetchApi<any>('/shipments?size=50')
      setShipments(pageData.content || [])
    } catch (error) {
      console.error("Failed to fetch shipments:", error)
      toast.error("배송 목록을 불러오지 못했습니다.")
    } finally {
      setLoading(false)
    }
  }, [])

  useEffect(() => {
    loadShipments()
  }, [loadShipments])

  const handleStart = async (shipmentId: string) => {
    try {
      await fetchApi(`/shipments/${shipmentId}/start`, { method: 'POST' })
      toast.success("배송이 시작되었습니다.")
      loadShipments()
    } catch (error: any) {
      toast.error(error.message || "배송 시작에 실패했습니다.")
    }
  }

  const handleDeliver = async (shipmentId: string) => {
    if (!confirm("배송 완료 처리하시겠습니까?")) return
    try {
      await fetchApi(`/shipments/${shipmentId}/delivered`, { method: 'PATCH' })
      toast.success("배송 완료 처리되었습니다.")
      loadShipments()
    } catch (error: any) {
      toast.error(error.message || "배송 완료 처리에 실패했습니다.")
    }
  }

  const handleDelete = async (shipmentId: string) => {
    if (!confirm("정말 이 배송을 삭제하시겠습니까?")) return
    try {
      await fetchApi(`/shipments/${shipmentId}`, { method: 'DELETE' })
      toast.success("배송이 삭제되었습니다.")
      loadShipments()
    } catch (error: any) {
      toast.error(error.message || "삭제에 실패했습니다.")
    }
  }

  const handleViewDetail = (shipment: any) => {
    setSelectedShipment(shipment)
    setDetailOpen(true)
  }

  const filteredShipments = shipments.filter((s) => {
    const matchesSearch =
      (s.shipmentNumber || "").toLowerCase().includes(searchTerm.toLowerCase()) ||
      (s.recipientName || "").toLowerCase().includes(searchTerm.toLowerCase()) ||
      (s.deliveryAddress || "").toLowerCase().includes(searchTerm.toLowerCase())
    const matchesStatus = statusFilter === "all" || s.status === statusFilter
    return matchesSearch && matchesStatus
  })

  const formatDate = (dateStr: string) => {
    if (!dateStr) return "-"
    return new Date(dateStr).toLocaleDateString("ko-KR", {
      month: "2-digit", day: "2-digit", hour: "2-digit", minute: "2-digit"
    })
  }

  const deliveredCount = shipments.filter((s) => s.status === "DELIVERED").length
  const inProgressCount = shipments.filter((s) =>
    ["HUB_MOVING", "DELIVERING", "COMPANY_MOVING", "DESTINATION_HUB_ARRIVED"].includes(s.status)
  ).length
  const waitingCount = shipments.filter((s) => s.status === "HUB_WAITING").length

  return (
    <DashboardLayout>
      <div className="space-y-6">
        <div>
          <h1 className="text-2xl font-bold tracking-tight">배송 관리</h1>
          <p className="text-muted-foreground">진행 중인 배송 현황을 조회하고 관리합니다.</p>
        </div>

        <div className="grid gap-4 md:grid-cols-4">
          <Card className="bg-card border-border">
            <CardContent className="p-6">
              <div className="text-sm text-muted-foreground">전체 배송</div>
              <div className="text-3xl font-bold mt-1">{shipments.length}</div>
            </CardContent>
          </Card>
          <Card className="bg-card border-border">
            <CardContent className="p-6">
              <div className="text-sm text-muted-foreground">허브 대기</div>
              <div className="text-3xl font-bold mt-1 text-muted-foreground">{waitingCount}</div>
            </CardContent>
          </Card>
          <Card className="bg-card border-border">
            <CardContent className="p-6">
              <div className="text-sm text-muted-foreground">이동중</div>
              <div className="text-3xl font-bold mt-1 text-warning">{inProgressCount}</div>
            </CardContent>
          </Card>
          <Card className="bg-card border-border">
            <CardContent className="p-6">
              <div className="text-sm text-muted-foreground">배송완료</div>
              <div className="text-3xl font-bold mt-1 text-success">{deliveredCount}</div>
            </CardContent>
          </Card>
        </div>

        <Card className="bg-card border-border">
          <CardHeader className="pb-4">
            <div className="flex items-center justify-between">
              <div className="flex items-center gap-2 flex-wrap">
                <CardTitle className="text-lg">배송 목록</CardTitle>
                <div className="flex gap-1 ml-4 flex-wrap">
                  {["all", "HUB_WAITING", "HUB_MOVING", "DELIVERING", "DELIVERED", "CANCELLED"].map((status) => (
                    <Button
                      key={status}
                      variant={statusFilter === status ? "default" : "ghost"}
                      size="sm"
                      onClick={() => setStatusFilter(status)}
                      className={statusFilter === status ? "bg-primary" : ""}
                    >
                      {status === "all" ? "전체" : shipmentStatusConfig[status]?.label || status}
                    </Button>
                  ))}
                </div>
              </div>
              <div className="relative w-72">
                <Search className="absolute left-3 top-1/2 -translate-y-1/2 w-4 h-4 text-muted-foreground" />
                <Input
                  placeholder="배송번호, 수령인 검색..."
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
                  <TableHead className="text-muted-foreground">배송번호</TableHead>
                  <TableHead className="text-muted-foreground">수령인</TableHead>
                  <TableHead className="text-muted-foreground">배송지</TableHead>
                  <TableHead className="text-muted-foreground">예정 배송일</TableHead>
                  <TableHead className="text-muted-foreground">상태</TableHead>
                  <TableHead className="text-muted-foreground w-12"></TableHead>
                </TableRow>
              </TableHeader>
              <TableBody>
                {loading ? (
                  <TableRow>
                    <TableCell colSpan={6} className="text-center py-8 text-muted-foreground">
                      데이터를 불러오는 중입니다...
                    </TableCell>
                  </TableRow>
                ) : filteredShipments.length === 0 ? (
                  <TableRow>
                    <TableCell colSpan={6} className="text-center py-8 text-muted-foreground">
                      배송 정보가 없습니다. 주문을 생성하면 배송이 자동으로 생성됩니다.
                    </TableCell>
                  </TableRow>
                ) : (
                  filteredShipments.map((shipment, index) => (
                    <TableRow key={shipment.shipmentId || `shp-${index}`} className="border-border hover:bg-muted/50">
                      <TableCell className="font-mono text-sm text-primary">
                        {shipment.shipmentNumber || shipment.shipmentId?.slice(0, 8) + '...'}
                      </TableCell>
                      <TableCell>
                        <div>
                          <p className="font-medium">{shipment.recipientName || "-"}</p>
                          <p className="text-xs text-muted-foreground font-mono">{shipment.recipientSlackId || "-"}</p>
                        </div>
                      </TableCell>
                      <TableCell className="text-muted-foreground text-sm max-w-xs truncate">
                        {shipment.deliveryAddress || "-"}
                      </TableCell>
                      <TableCell className="text-muted-foreground text-sm">
                        {formatDate(shipment.estimatedDeliveryAt)}
                      </TableCell>
                      <TableCell>
                        <Badge
                          variant="outline"
                          className={cn(shipmentStatusConfig[shipment.status]?.className)}
                        >
                          {shipmentStatusConfig[shipment.status]?.label || shipment.status}
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
                            <DropdownMenuItem onClick={() => handleViewDetail(shipment)}>
                              <Eye className="w-4 h-4 mr-2" />
                              상세보기
                            </DropdownMenuItem>
                            {shipment.status === "HUB_WAITING" && (
                              <DropdownMenuItem
                                className="text-info cursor-pointer"
                                onClick={() => handleStart(shipment.shipmentId)}
                              >
                                <PlayCircle className="w-4 h-4 mr-2" />
                                배송 시작
                              </DropdownMenuItem>
                            )}
                            {["HUB_MOVING", "DELIVERING", "COMPANY_MOVING", "DESTINATION_HUB_ARRIVED"].includes(shipment.status) && (
                              <DropdownMenuItem
                                className="text-success cursor-pointer"
                                onClick={() => handleDeliver(shipment.shipmentId)}
                              >
                                <CheckCircle className="w-4 h-4 mr-2" />
                                배송 완료
                              </DropdownMenuItem>
                            )}
                            <DropdownMenuItem
                              className="text-destructive cursor-pointer"
                              onClick={() => handleDelete(shipment.shipmentId)}
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

      {/* 배송 상세 다이얼로그 */}
      <Dialog open={detailOpen} onOpenChange={setDetailOpen}>
        <DialogContent className="sm:max-w-[600px] max-h-[90vh] overflow-y-auto">
          <DialogHeader>
            <DialogTitle>배송 상세 정보</DialogTitle>
          </DialogHeader>
          {selectedShipment && (
            <div className="space-y-4">
              <div className="grid grid-cols-2 gap-4">
                <div>
                  <p className="text-xs text-muted-foreground">배송번호</p>
                  <p className="font-mono text-sm">{selectedShipment.shipmentNumber || selectedShipment.shipmentId?.slice(0, 12) + '...'}</p>
                </div>
                <div>
                  <p className="text-xs text-muted-foreground">상태</p>
                  <Badge
                    variant="outline"
                    className={shipmentStatusConfig[selectedShipment.status]?.className}
                  >
                    {shipmentStatusConfig[selectedShipment.status]?.label || selectedShipment.status}
                  </Badge>
                </div>
                <div>
                  <p className="text-xs text-muted-foreground">수령인</p>
                  <p className="font-medium">{selectedShipment.recipientName || "-"}</p>
                </div>
                <div>
                  <p className="text-xs text-muted-foreground">수령인 Slack ID</p>
                  <p className="font-mono text-sm">{selectedShipment.recipientSlackId || "-"}</p>
                </div>
                <div className="col-span-2">
                  <p className="text-xs text-muted-foreground">배송지 주소</p>
                  <p className="text-sm">{selectedShipment.deliveryAddress || "-"}</p>
                </div>
                <div>
                  <p className="text-xs text-muted-foreground">예정 배송일</p>
                  <p className="text-sm">{formatDate(selectedShipment.estimatedDeliveryAt)}</p>
                </div>
                <div>
                  <p className="text-xs text-muted-foreground">배송 완료일</p>
                  <p className="text-sm">{formatDate(selectedShipment.deliveredAt)}</p>
                </div>
              </div>

              {selectedShipment.histories && selectedShipment.histories.length > 0 && (
                <div>
                  <p className="text-sm font-medium mb-2">배송 경로 기록</p>
                  <div className="space-y-2">
                    {selectedShipment.histories.map((history: any, i: number) => (
                      <div key={i} className="flex items-center gap-2 text-sm p-3 rounded bg-muted/50">
                        <span className="font-mono text-xs text-muted-foreground w-6">{i + 1}</span>
                        <span className="text-muted-foreground truncate">
                          {history.originHubId?.slice(0, 8)}...
                        </span>
                        <ArrowRight className="w-3 h-3 text-muted-foreground shrink-0" />
                        <span className="text-muted-foreground truncate">
                          {history.destinationHubId?.slice(0, 8)}...
                        </span>
                        <Badge variant="outline" className="text-xs ml-auto shrink-0">
                          {history.status || "대기"}
                        </Badge>
                      </div>
                    ))}
                  </div>
                </div>
              )}
            </div>
          )}
        </DialogContent>
      </Dialog>
    </DashboardLayout>
  )
}
