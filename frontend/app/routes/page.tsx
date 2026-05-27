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
import { Plus, Search, MoreHorizontal, Pencil, Trash2, ArrowRight } from "lucide-react"
import { useState } from "react"

const routes = [
  { id: "RTE-001", from: "서울특별시 센터", to: "경기 남부 센터", distance: 45, duration: 55 },
  { id: "RTE-002", from: "경기 남부 센터", to: "대전광역시 센터", distance: 120, duration: 90 },
  { id: "RTE-003", from: "대전광역시 센터", to: "대구광역시 센터", distance: 130, duration: 100 },
  { id: "RTE-004", from: "대구광역시 센터", to: "부산광역시 센터", distance: 90, duration: 70 },
  { id: "RTE-005", from: "서울특별시 센터", to: "인천광역시 센터", distance: 35, duration: 45 },
  { id: "RTE-006", from: "경기 북부 센터", to: "서울특별시 센터", distance: 25, duration: 35 },
  { id: "RTE-007", from: "대전광역시 센터", to: "광주광역시 센터", distance: 150, duration: 120 },
  { id: "RTE-008", from: "대구광역시 센터", to: "울산광역시 센터", distance: 70, duration: 55 },
  { id: "RTE-009", from: "경기 남부 센터", to: "강원특별자치도 센터", distance: 110, duration: 85 },
  { id: "RTE-010", from: "대전광역시 센터", to: "세종특별자치시 센터", distance: 20, duration: 25 },
  { id: "RTE-011", from: "대전광역시 센터", to: "충청북도 센터", distance: 60, duration: 50 },
  { id: "RTE-012", from: "대전광역시 센터", to: "충청남도 센터", distance: 55, duration: 45 },
  { id: "RTE-013", from: "광주광역시 센터", to: "전북특별자치도 센터", distance: 80, duration: 65 },
  { id: "RTE-014", from: "광주광역시 센터", to: "전라남도 센터", distance: 50, duration: 40 },
  { id: "RTE-015", from: "대구광역시 센터", to: "경상북도 센터", distance: 65, duration: 55 },
  { id: "RTE-016", from: "부산광역시 센터", to: "경상남도 센터", distance: 45, duration: 40 },
]

export default function RoutesPage() {
  const [searchTerm, setSearchTerm] = useState("")

  const filteredRoutes = routes.filter(
    (route) =>
      route.from.toLowerCase().includes(searchTerm.toLowerCase()) ||
      route.to.toLowerCase().includes(searchTerm.toLowerCase())
  )

  return (
    <DashboardLayout>
      <div className="space-y-6">
        <div className="flex items-center justify-between">
          <div>
            <h1 className="text-2xl font-bold tracking-tight">이동경로 관리</h1>
            <p className="text-muted-foreground">허브 간 이동 경로 및 소요 시간을 관리합니다.</p>
          </div>
          <Button className="bg-primary hover:bg-primary/90">
            <Plus className="w-4 h-4 mr-2" />
            경로 추가
          </Button>
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
              <div className="text-sm text-muted-foreground">평균 거리</div>
              <div className="text-3xl font-bold mt-1">
                {Math.round(routes.reduce((acc, r) => acc + r.distance, 0) / routes.length)}km
              </div>
            </CardContent>
          </Card>
          <Card className="bg-card border-border">
            <CardContent className="p-6">
              <div className="text-sm text-muted-foreground">평균 소요시간</div>
              <div className="text-3xl font-bold mt-1">
                {Math.round(routes.reduce((acc, r) => acc + r.duration, 0) / routes.length)}분
              </div>
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
                  <TableHead className="text-muted-foreground">경로 ID</TableHead>
                  <TableHead className="text-muted-foreground">출발 허브</TableHead>
                  <TableHead className="text-muted-foreground w-12"></TableHead>
                  <TableHead className="text-muted-foreground">도착 허브</TableHead>
                  <TableHead className="text-muted-foreground">거리</TableHead>
                  <TableHead className="text-muted-foreground">소요시간</TableHead>
                  <TableHead className="text-muted-foreground w-12"></TableHead>
                </TableRow>
              </TableHeader>
              <TableBody>
                {filteredRoutes.map((route) => (
                  <TableRow key={route.id} className="border-border hover:bg-muted/50">
                    <TableCell className="font-mono text-sm text-primary">{route.id}</TableCell>
                    <TableCell className="font-medium">{route.from}</TableCell>
                    <TableCell>
                      <ArrowRight className="w-4 h-4 text-muted-foreground" />
                    </TableCell>
                    <TableCell className="font-medium">{route.to}</TableCell>
                    <TableCell>
                      <span className="font-mono">{route.distance}</span>
                      <span className="text-muted-foreground ml-1">km</span>
                    </TableCell>
                    <TableCell>
                      <span className="font-mono">{route.duration}</span>
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
                          <DropdownMenuItem className="text-destructive">
                            <Trash2 className="w-4 h-4 mr-2" />
                            삭제
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
