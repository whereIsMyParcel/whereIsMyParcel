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
import { Avatar, AvatarFallback } from "@/components/ui/avatar"
import { Plus, Search, MoreHorizontal, Pencil, Trash2 } from "lucide-react"
import { useState, useEffect } from "react"
import { fetchApi } from "@/lib/api"

export default function DriversPage() {
  const [drivers, setDrivers] = useState<any[]>([])
  const [loading, setLoading] = useState(true)
  const [searchTerm, setSearchTerm] = useState("")
  const [filterType, setFilterType] = useState<"all" | "hub" | "company">("all")

  useEffect(() => {
    async function loadDrivers() {
      try {
        setLoading(true)
        const pageData = await fetchApi<any>('/delivery-managers?size=50')
        setDrivers(pageData.content || [])
      } catch (error) {
        console.error("Failed to fetch drivers:", error)
      } finally {
        setLoading(false)
      }
    }
    loadDrivers()
  }, [])

  const filteredDrivers = drivers.filter((driver) => {
    const matchesSearch =
      driver.name.toLowerCase().includes(searchTerm.toLowerCase()) ||
      driver.slackId.toLowerCase().includes(searchTerm.toLowerCase())
    const matchesType = filterType === "all" || driver.type === filterType
    return matchesSearch && matchesType
  })

  const hubDrivers = drivers.filter((d) => d.type === "hub")
  const companyDrivers = drivers.filter((d) => d.type === "company")

  return (
    <DashboardLayout>
      <div className="space-y-6">
        <div className="flex items-center justify-between">
          <div>
            <h1 className="text-2xl font-bold tracking-tight">배송담당자 관리</h1>
            <p className="text-muted-foreground">허브 및 업체 배송 담당자를 관리합니다.</p>
          </div>
          <Button className="bg-primary hover:bg-primary/90">
            <Plus className="w-4 h-4 mr-2" />
            담당자 추가
          </Button>
        </div>

        <div className="grid gap-4 md:grid-cols-3">
          <Card className="bg-card border-border">
            <CardContent className="p-6">
              <div className="text-sm text-muted-foreground">전체 담당자</div>
              <div className="text-3xl font-bold mt-1">{drivers.length}명</div>
            </CardContent>
          </Card>
          <Card className="bg-card border-border">
            <CardContent className="p-6">
              <div className="text-sm text-muted-foreground">허브 배송 담당자</div>
              <div className="text-3xl font-bold mt-1">{hubDrivers.length}명</div>
              <p className="text-xs text-muted-foreground mt-1">허브 간 이동 담당</p>
            </CardContent>
          </Card>
          <Card className="bg-card border-border">
            <CardContent className="p-6">
              <div className="text-sm text-muted-foreground">업체 배송 담당자</div>
              <div className="text-3xl font-bold mt-1">{companyDrivers.length}명</div>
              <p className="text-xs text-muted-foreground mt-1">허브 → 업체 배송 담당</p>
            </CardContent>
          </Card>
        </div>

        <Card className="bg-card border-border">
          <CardHeader className="pb-4">
            <div className="flex items-center justify-between">
              <div className="flex items-center gap-2">
                <CardTitle className="text-lg">담당자 목록</CardTitle>
                <div className="flex gap-1 ml-4">
                  <Button
                    variant={filterType === "all" ? "default" : "ghost"}
                    size="sm"
                    onClick={() => setFilterType("all")}
                    className={filterType === "all" ? "bg-primary" : ""}
                  >
                    전체
                  </Button>
                  <Button
                    variant={filterType === "hub" ? "default" : "ghost"}
                    size="sm"
                    onClick={() => setFilterType("hub")}
                    className={filterType === "hub" ? "bg-primary" : ""}
                  >
                    허브 담당자
                  </Button>
                  <Button
                    variant={filterType === "company" ? "default" : "ghost"}
                    size="sm"
                    onClick={() => setFilterType("company")}
                    className={filterType === "company" ? "bg-primary" : ""}
                  >
                    업체 담당자
                  </Button>
                </div>
              </div>
              <div className="relative w-72">
                <Search className="absolute left-3 top-1/2 -translate-y-1/2 w-4 h-4 text-muted-foreground" />
                <Input
                  placeholder="이름 또는 Slack ID 검색..."
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
                  <TableHead className="text-muted-foreground">담당자 ID</TableHead>
                  <TableHead className="text-muted-foreground">이름</TableHead>
                  <TableHead className="text-muted-foreground">유형</TableHead>
                  <TableHead className="text-muted-foreground">소속 허브</TableHead>
                  <TableHead className="text-muted-foreground">Slack ID</TableHead>
                  <TableHead className="text-muted-foreground">배송 순번</TableHead>
                  <TableHead className="text-muted-foreground">상태</TableHead>
                  <TableHead className="text-muted-foreground w-12"></TableHead>
                </TableRow>
              </TableHeader>
              <TableBody>
                {loading ? (
                  <TableRow>
                    <TableCell colSpan={8} className="text-center py-8 text-muted-foreground">
                      데이터를 불러오는 중입니다...
                    </TableCell>
                  </TableRow>
                ) : filteredDrivers.length === 0 ? (
                  <TableRow>
                    <TableCell colSpan={8} className="text-center py-8 text-muted-foreground">
                      등록된 배송담당자가 없습니다.
                    </TableCell>
                  </TableRow>
                ) : (
                  filteredDrivers.map((driver, index) => (
                    <TableRow key={driver.id || `drv-${index}`} className="border-border hover:bg-muted/50">
                    <TableCell className="font-mono text-sm text-primary">{driver.id}</TableCell>
                    <TableCell>
                      <div className="flex items-center gap-2">
                        <Avatar className="h-8 w-8">
                          <AvatarFallback className="bg-primary/20 text-primary text-xs">
                            {driver.name.slice(0, 2)}
                          </AvatarFallback>
                        </Avatar>
                        <span className="font-medium">{driver.name}</span>
                      </div>
                    </TableCell>
                    <TableCell>
                      <Badge
                        variant="outline"
                        className={
                          driver.type === "hub"
                            ? "bg-info/20 text-info border-info/30"
                            : "bg-warning/20 text-warning border-warning/30"
                        }
                      >
                        {driver.type === "hub" ? "허브 담당자" : "업체 담당자"}
                      </Badge>
                    </TableCell>
                    <TableCell className="text-muted-foreground">
                      {driver.hub || "-"}
                    </TableCell>
                    <TableCell className="font-mono text-sm">{driver.slackId}</TableCell>
                    <TableCell className="font-mono">{driver.order}</TableCell>
                    <TableCell>
                      <Badge
                        variant="outline"
                        className={
                          driver.status === "active"
                            ? "bg-success/20 text-success border-success/30"
                            : "bg-muted text-muted-foreground border-muted"
                        }
                      >
                        {driver.status === "active" ? "활성" : "비활성"}
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
                            <Pencil className="w-4 h-4 mr-2" />
                            수정
                          </DropdownMenuItem>
                          <DropdownMenuItem className="text-destructive">
                            <Trash2 className="w-4 h-4 mr-2" />
                            삭제
                          </DropdownMenuItem>
                        </DropdownMenuContent>
                      </DropdownMenu>
                    </TableCell>
                  </TableRow>
                )))}
              </TableBody>
            </Table>
          </CardContent>
        </Card>
      </div>
    </DashboardLayout>
  )
}
