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
import { useState, useEffect, useCallback } from "react"
import { fetchApi } from "@/lib/api"
import { CreateDriverModal } from "@/components/drivers/create-driver-modal"
import { toast } from "sonner"

// DeliveryType: HUB_DELIVERY, COMPANY_DELIVERY
const driverTypeConfig: Record<string, { label: string; className: string }> = {
  HUB_DELIVERY: { label: "허브 담당자", className: "bg-info/20 text-info border-info/30" },
  COMPANY_DELIVERY: { label: "업체 담당자", className: "bg-warning/20 text-warning border-warning/30" },
}

export default function DriversPage() {
  const [drivers, setDrivers] = useState<any[]>([])
  const [hubs, setHubs] = useState<Record<string, string>>({})
  const [loading, setLoading] = useState(true)
  const [searchTerm, setSearchTerm] = useState("")
  const [filterType, setFilterType] = useState<"all" | "HUB_DELIVERY" | "COMPANY_DELIVERY">("all")
  const [isCreateModalOpen, setIsCreateModalOpen] = useState(false)

  const loadHubs = useCallback(async () => {
    try {
      const data = await fetchApi<any>('/hubs?size=50')
      const hubMap: Record<string, string> = {}
      ;(data.content || []).forEach((h: any) => { hubMap[h.hubId] = h.name })
      setHubs(hubMap)
    } catch (error) {
      console.error("Failed to fetch hubs:", error)
    }
  }, [])

  const loadDrivers = useCallback(async () => {
    try {
      setLoading(true)
      const pageData = await fetchApi<any>('/delivery-managers?size=50')
      setDrivers(pageData.content || [])
    } catch (error) {
      console.error("Failed to fetch drivers:", error)
      toast.error("배송담당자 목록을 불러오지 못했습니다.")
    } finally {
      setLoading(false)
    }
  }, [])

  useEffect(() => {
    loadHubs()
    loadDrivers()
  }, [loadHubs, loadDrivers])

  const handleDelete = async (id: string) => {
    if (!confirm("정말로 이 담당자를 삭제하시겠습니까?")) return
    try {
      await fetchApi(`/delivery-managers/${id}`, { method: 'DELETE' })
      toast.success("담당자가 삭제되었습니다.")
      loadDrivers()
    } catch (error: any) {
      toast.error(error.message || "삭제 실패")
    }
  }

  const filteredDrivers = drivers.filter((driver) => {
    const slackId = driver.slackId || ""
    const hubName = hubs[driver.hubId] || driver.hubId || ""
    const matchesSearch =
      slackId.toLowerCase().includes(searchTerm.toLowerCase()) ||
      hubName.toLowerCase().includes(searchTerm.toLowerCase())
    const matchesType = filterType === "all" || driver.type === filterType
    return matchesSearch && matchesType
  })

  const hubDrivers = drivers.filter((d) => d.type === "HUB_DELIVERY")
  const companyDrivers = drivers.filter((d) => d.type === "COMPANY_DELIVERY")

  return (
    <DashboardLayout>
      <div className="space-y-6">
        <div className="flex items-center justify-between">
          <div>
            <h1 className="text-2xl font-bold tracking-tight">배송담당자 관리</h1>
            <p className="text-muted-foreground">허브 및 업체 배송 담당자를 관리합니다.</p>
          </div>
          <Button className="bg-primary hover:bg-primary/90" onClick={() => setIsCreateModalOpen(true)}>
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
                  {(["all", "HUB_DELIVERY", "COMPANY_DELIVERY"] as const).map((type) => (
                    <Button
                      key={type}
                      variant={filterType === type ? "default" : "ghost"}
                      size="sm"
                      onClick={() => setFilterType(type)}
                      className={filterType === type ? "bg-primary" : ""}
                    >
                      {type === "all" ? "전체" : driverTypeConfig[type]?.label}
                    </Button>
                  ))}
                </div>
              </div>
              <div className="relative w-72">
                <Search className="absolute left-3 top-1/2 -translate-y-1/2 w-4 h-4 text-muted-foreground" />
                <Input
                  placeholder="Slack ID 또는 허브명 검색..."
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
                  <TableHead className="text-muted-foreground">Slack ID</TableHead>
                  <TableHead className="text-muted-foreground">유형</TableHead>
                  <TableHead className="text-muted-foreground">소속 허브</TableHead>
                  <TableHead className="text-muted-foreground">배송 순번</TableHead>
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
                ) : filteredDrivers.length === 0 ? (
                  <TableRow>
                    <TableCell colSpan={6} className="text-center py-8 text-muted-foreground">
                      등록된 배송담당자가 없습니다.
                    </TableCell>
                  </TableRow>
                ) : (
                  filteredDrivers.map((driver, index) => (
                    <TableRow key={driver.deliveryManagerId || `drv-${index}`} className="border-border hover:bg-muted/50">
                      <TableCell className="font-mono text-xs text-primary">
                        {driver.deliveryManagerId?.slice(0, 8) + '...' || '-'}
                      </TableCell>
                      <TableCell>
                        <div className="flex items-center gap-2">
                          <Avatar className="h-8 w-8">
                            <AvatarFallback className="bg-primary/20 text-primary text-xs">
                              {(driver.slackId || "??").slice(0, 2).toUpperCase()}
                            </AvatarFallback>
                          </Avatar>
                          <span className="font-mono text-sm">{driver.slackId || "-"}</span>
                        </div>
                      </TableCell>
                      <TableCell>
                        <Badge
                          variant="outline"
                          className={driverTypeConfig[driver.type]?.className}
                        >
                          {driverTypeConfig[driver.type]?.label || driver.type}
                        </Badge>
                      </TableCell>
                      <TableCell className="text-muted-foreground">
                        {driver.hubId ? (hubs[driver.hubId] || driver.hubId.slice(0, 8) + '...') : "전체"}
                      </TableCell>
                      <TableCell className="font-mono">{driver.deliveryOrder ?? "-"}</TableCell>
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
                            <DropdownMenuItem className="text-destructive cursor-pointer" onClick={() => handleDelete(driver.deliveryManagerId)}>
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
      <CreateDriverModal
        open={isCreateModalOpen}
        onOpenChange={setIsCreateModalOpen}
        onSuccess={loadDrivers}
      />
    </DashboardLayout>
  )
}
