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
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card"
import {
  DropdownMenu,
  DropdownMenuContent,
  DropdownMenuItem,
  DropdownMenuTrigger,
} from "@/components/ui/dropdown-menu"
import { Plus, Search, MoreHorizontal, Pencil, Trash2, ArrowRight, Clock, Ruler } from "lucide-react"
import { useState, useEffect, useCallback } from "react"
import { fetchApi } from "@/lib/api"
import { CreateRouteModal } from "@/components/routes/create-route-modal"
import { toast } from "sonner"

export default function RoutesPage() {
  const [routes, setRoutes] = useState<any[]>([])
  const [loading, setLoading] = useState(true)
  const [searchTerm, setSearchTerm] = useState("")

  const loadRoutes = useCallback(async () => {
    try {
      setLoading(true)
      const pageData = await fetchApi<any>('/hub-routes?size=50')
      setRoutes(pageData.content || [])
    } catch (error) {
      console.error("Failed to fetch routes:", error)
      toast.error("경로 목록을 불러오지 못했습니다.")
    } finally {
      setLoading(false)
    }
  }, [])

  useEffect(() => {
    loadRoutes()
  }, [loadRoutes])

  const handleDelete = async (hubRouteId: string) => {
    if (!confirm("정말 이 경로를 삭제하시겠습니까?")) return
    try {
      await fetchApi(`/hub-routes/${hubRouteId}`, { method: 'DELETE' })
      toast.success("경로가 삭제되었습니다.")
      loadRoutes()
    } catch (error: any) {
      toast.error(error.message || "삭제에 실패했습니다.")
    }
  }

  const filteredRoutes = routes.filter(
    (route) =>
      (route.originHubName || "").toLowerCase().includes(searchTerm.toLowerCase()) ||
      (route.destinationHubName || "").toLowerCase().includes(searchTerm.toLowerCase())
  )

  const avgDistance = routes.length > 0
    ? Math.round(routes.reduce((acc, r) => acc + (r.distance || 0), 0) / routes.length)
    : 0
  const avgDuration = routes.length > 0
    ? Math.round(routes.reduce((acc, r) => acc + (r.duration || 0), 0) / routes.length)
    : 0

  return (
    <DashboardLayout>
      <div className="space-y-6">
        <div className="flex items-center justify-between">
          <div>
            <h1 className="text-2xl font-bold tracking-tight">이동경로 관리</h1>
            <p className="text-muted-foreground">허브 간 이동 경로 및 소요 시간을 관리합니다.</p>
          </div>
          <CreateRouteModal onSuccess={loadRoutes} />
        </div>

        <div className="grid gap-4 md:grid-cols-3">
          <Card className="bg-card border-border">
            <CardContent className="p-6">
              <div className="text-sm text-muted-foreground">전체 경로</div>
              <div className="text-3xl font-bold mt-1">{routes.length}</div>
            </CardContent>
          </Card>
          <Card className="bg-card border-border">
            <CardContent className="p-6">
              <div className="flex items-center gap-2 text-sm text-muted-foreground">
                <Ruler className="w-4 h-4" />
                평균 거리
              </div>
              <div className="text-3xl font-bold mt-1">{avgDistance}<span className="text-lg text-muted-foreground ml-1">km</span></div>
            </CardContent>
          </Card>
          <Card className="bg-card border-border">
            <CardContent className="p-6">
              <div className="flex items-center gap-2 text-sm text-muted-foreground">
                <Clock className="w-4 h-4" />
                평균 소요시간
              </div>
              <div className="text-3xl font-bold mt-1">{avgDuration}<span className="text-lg text-muted-foreground ml-1">분</span></div>
            </CardContent>
          </Card>
        </div>

        <Card className="bg-card border-border">
          <CardHeader className="pb-4">
            <div className="flex items-center justify-between">
              <CardTitle className="text-lg">경로 목록</CardTitle>
              <div className="relative w-72">
                <Search className="absolute left-3 top-1/2 -translate-y-1/2 w-4 h-4 text-muted-foreground" />
                <Input
                  placeholder="출발/도착 허브 검색..."
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
                  <TableHead className="text-muted-foreground">출발 허브</TableHead>
                  <TableHead className="text-muted-foreground w-8"></TableHead>
                  <TableHead className="text-muted-foreground">도착 허브</TableHead>
                  <TableHead className="text-muted-foreground">거리</TableHead>
                  <TableHead className="text-muted-foreground">소요시간</TableHead>
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
                ) : filteredRoutes.length === 0 ? (
                  <TableRow>
                    <TableCell colSpan={6} className="text-center py-8 text-muted-foreground">
                      등록된 경로가 없습니다.
                    </TableCell>
                  </TableRow>
                ) : (
                  filteredRoutes.map((route, index) => (
                    <TableRow key={route.hubRouteId || `rte-${index}`} className="border-border hover:bg-muted/50">
                      <TableCell className="font-medium">{route.originHubName || route.originHubId?.slice(0, 8) + '...' || '-'}</TableCell>
                      <TableCell>
                        <ArrowRight className="w-4 h-4 text-muted-foreground" />
                      </TableCell>
                      <TableCell className="font-medium">{route.destinationHubName || route.destinationHubId?.slice(0, 8) + '...' || '-'}</TableCell>
                      <TableCell>
                        <span className="font-mono">{route.distance?.toFixed(1) || '-'}</span>
                        <span className="text-muted-foreground ml-1">km</span>
                      </TableCell>
                      <TableCell>
                        <span className="font-mono">{route.duration || '-'}</span>
                        <span className="text-muted-foreground ml-1">분</span>
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
                            <DropdownMenuItem
                              className="text-destructive cursor-pointer"
                              onClick={() => handleDelete(route.hubRouteId)}
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
    </DashboardLayout>
  )
}
