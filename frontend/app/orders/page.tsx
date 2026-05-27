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
import { Plus, Search, MoreHorizontal, Eye, Trash2, ArrowRight } from "lucide-react"
import { useState } from "react"
import { cn } from "@/lib/utils"

const orders = [
  {
    id: "ORD-20240101-001",
    product: "마른오징어 가공품",
    quantity: 50,
    supplier: "경기 건조식품",
    receiver: "부산 수산물 도매",
    fromHub: "경기 남부 센터",
    toHub: "부산광역시 센터",
    status: "delivering",
    createdAt: "2024-01-15 09:30",
  },
  {
    id: "ORD-20240101-002",
    product: "플라스틱 가공품",
    quantity: 120,
    supplier: "서울 플라스틱",
    receiver: "대구 제조업체",
    fromHub: "서울특별시 센터",
    toHub: "대구광역시 센터",
    status: "preparing",
    createdAt: "2024-01-15 10:15",
  },
  {
    id: "ORD-20240101-003",
    product: "전자부품 세트",
    quantity: 80,
    supplier: "인천 전자",
    receiver: "광주 조립업체",
    fromHub: "인천광역시 센터",
    toHub: "광주광역시 센터",
    status: "completed",
    createdAt: "2024-01-14 14:20",
  },
  {
    id: "ORD-20240101-004",
    product: "식품 원재료",
    quantity: 200,
    supplier: "대전 식품",
    receiver: "울산 가공업체",
    fromHub: "대전광역시 센터",
    toHub: "울산광역시 센터",
    status: "delivering",
    createdAt: "2024-01-15 08:00",
  },
  {
    id: "ORD-20240101-005",
    product: "의류 원단",
    quantity: 150,
    supplier: "경기 섬유",
    receiver: "경남 의류업체",
    fromHub: "경기 북부 센터",
    toHub: "경상남도 센터",
    status: "preparing",
    createdAt: "2024-01-15 11:45",
  },
  {
    id: "ORD-20240101-006",
    product: "화장품 원료",
    quantity: 75,
    supplier: "서울 화학",
    receiver: "전북 화장품",
    fromHub: "서울특별시 센터",
    toHub: "전북특별자치도 센터",
    status: "completed",
    createdAt: "2024-01-13 16:30",
  },
  {
    id: "ORD-20240101-007",
    product: "농산물 가공품",
    quantity: 300,
    supplier: "충남 농산",
    receiver: "강원 유통",
    fromHub: "충청남도 센터",
    toHub: "강원특별자치도 센터",
    status: "cancelled",
    createdAt: "2024-01-12 09:00",
  },
]

const statusConfig = {
  preparing: { label: "준비중", className: "bg-warning/20 text-warning border-warning/30" },
  delivering: { label: "배송중", className: "bg-info/20 text-info border-info/30" },
  completed: { label: "완료", className: "bg-success/20 text-success border-success/30" },
  cancelled: { label: "취소", className: "bg-destructive/20 text-destructive border-destructive/30" },
}

export default function OrdersPage() {
  const [searchTerm, setSearchTerm] = useState("")
  const [statusFilter, setStatusFilter] = useState<string>("all")

  const filteredOrders = orders.filter((order) => {
    const matchesSearch =
      order.id.toLowerCase().includes(searchTerm.toLowerCase()) ||
      order.product.toLowerCase().includes(searchTerm.toLowerCase()) ||
      order.supplier.toLowerCase().includes(searchTerm.toLowerCase()) ||
      order.receiver.toLowerCase().includes(searchTerm.toLowerCase())
    const matchesStatus = statusFilter === "all" || order.status === statusFilter
    return matchesSearch && matchesStatus
  })

  const statusCounts = {
    preparing: orders.filter((o) => o.status === "preparing").length,
    delivering: orders.filter((o) => o.status === "delivering").length,
    completed: orders.filter((o) => o.status === "completed").length,
  }

  return (
    <DashboardLayout>
      <div className="space-y-6">
        <div className="flex items-center justify-between">
          <div>
            <h1 className="text-2xl font-bold tracking-tight">주문 관리</h1>
            <p className="text-muted-foreground">배송 주문을 생성하고 관리합니다.</p>
          </div>
          <Button className="bg-primary hover:bg-primary/90">
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
              <div className="text-sm text-muted-foreground">준비중</div>
              <div className="text-3xl font-bold mt-1 text-warning">{statusCounts.preparing}</div>
            </CardContent>
          </Card>
          <Card className="bg-card border-border">
            <CardContent className="p-6">
              <div className="text-sm text-muted-foreground">배송중</div>
              <div className="text-3xl font-bold mt-1 text-info">{statusCounts.delivering}</div>
            </CardContent>
          </Card>
          <Card className="bg-card border-border">
            <CardContent className="p-6">
              <div className="text-sm text-muted-foreground">완료</div>
              <div className="text-3xl font-bold mt-1 text-success">{statusCounts.completed}</div>
            </CardContent>
          </Card>
        </div>

        <Card className="bg-card border-border">
          <CardHeader className="pb-4">
            <div className="flex items-center justify-between">
              <div className="flex items-center gap-2">
                <CardTitle className="text-lg">주문 목록</CardTitle>
                <div className="flex gap-1 ml-4">
                  {["all", "preparing", "delivering", "completed"].map((status) => (
                    <Button
                      key={status}
                      variant={statusFilter === status ? "default" : "ghost"}
                      size="sm"
                      onClick={() => setStatusFilter(status)}
                      className={statusFilter === status ? "bg-primary" : ""}
                    >
                      {status === "all" ? "전체" : statusConfig[status as keyof typeof statusConfig]?.label}
                    </Button>
                  ))}
                </div>
              </div>
              <div className="relative w-72">
                <Search className="absolute left-3 top-1/2 -translate-y-1/2 w-4 h-4 text-muted-foreground" />
                <Input
                  placeholder="주문번호, 상품, 업체 검색..."
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
                  <TableHead className="text-muted-foreground">상품</TableHead>
                  <TableHead className="text-muted-foreground">수량</TableHead>
                  <TableHead className="text-muted-foreground">경로</TableHead>
                  <TableHead className="text-muted-foreground">생성일</TableHead>
                  <TableHead className="text-muted-foreground">상태</TableHead>
                  <TableHead className="text-muted-foreground w-12"></TableHead>
                </TableRow>
              </TableHeader>
              <TableBody>
                {filteredOrders.map((order) => (
                  <TableRow key={order.id} className="border-border hover:bg-muted/50">
                    <TableCell className="font-mono text-sm text-primary">{order.id}</TableCell>
                    <TableCell>
                      <div>
                        <p className="font-medium">{order.product}</p>
                        <p className="text-xs text-muted-foreground">
                          {order.supplier} → {order.receiver}
                        </p>
                      </div>
                    </TableCell>
                    <TableCell className="font-mono">{order.quantity}개</TableCell>
                    <TableCell>
                      <div className="flex items-center gap-1 text-sm">
                        <span className="truncate max-w-24">{order.fromHub.replace(" 센터", "")}</span>
                        <ArrowRight className="w-3 h-3 text-muted-foreground shrink-0" />
                        <span className="truncate max-w-24">{order.toHub.replace(" 센터", "")}</span>
                      </div>
                    </TableCell>
                    <TableCell className="text-muted-foreground text-sm">{order.createdAt}</TableCell>
                    <TableCell>
                      <Badge
                        variant="outline"
                        className={cn(statusConfig[order.status as keyof typeof statusConfig]?.className)}
                      >
                        {statusConfig[order.status as keyof typeof statusConfig]?.label}
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
                          <DropdownMenuItem className="text-destructive">
                            <Trash2 className="w-4 h-4 mr-2" />
                            취소
                          </DropdownMenuItem>
                        </DropdownMenuContent>
                      </DropdownMenu>
                    </TableCell>
                  </TableRow>
                ))}
              </TableBody>
            </Table>
          </CardContent>
        </Card>
      </div>
    </DashboardLayout>
  )
}
