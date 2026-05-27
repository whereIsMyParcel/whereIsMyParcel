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
import { Plus, Search, MoreHorizontal, Pencil, Trash2, MapPin } from "lucide-react"
import { useState, useEffect, useCallback } from "react"
import { fetchApi } from "@/lib/api"
import { CreateHubModal } from "@/components/hubs/create-hub-modal"

export default function HubsPage() {
  const [searchTerm, setSearchTerm] = useState("")
  const [hubs, setHubs] = useState<any[]>([])
  const [loading, setLoading] = useState(true)

  const loadHubs = useCallback(async () => {
    try {
      setLoading(true);
      // 백엔드의 GET /api/v1/hubs 호출 (페이징 없이 100개 임시 로드)
      const pageData = await fetchApi<any>('/hubs?size=50');
      // Spring Boot의 Page 객체는 실제 리스트를 content 안에 담아 보냅니다.
      setHubs(pageData.content || []);
    } catch (error) {
      console.error("Failed to fetch hubs:", error);
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => {
    loadHubs();
  }, [loadHubs]);

  const filteredHubs = hubs.filter(
    (hub) =>
      hub.name?.toLowerCase().includes(searchTerm.toLowerCase()) ||
      hub.address?.toLowerCase().includes(searchTerm.toLowerCase())
  )

  return (
    <DashboardLayout>
      <div className="space-y-6">
        <div className="flex items-center justify-between">
          <div>
            <h1 className="text-2xl font-bold tracking-tight">허브 관리</h1>
            <p className="text-muted-foreground">스파르타 물류의 전체 허브 목록을 관리합니다.</p>
          </div>
          <CreateHubModal onSuccess={loadHubs} />
        </div>

        <Card className="bg-card border-border">
          <CardHeader className="pb-4">
            <div className="flex items-center justify-between">
              <CardTitle className="text-lg">허브 목록</CardTitle>
              <div className="relative w-72">
                <Search className="absolute left-3 top-1/2 -translate-y-1/2 w-4 h-4 text-muted-foreground" />
                <Input
                  placeholder="허브 이름 또는 주소 검색..."
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
                  <TableHead className="text-muted-foreground">허브 ID</TableHead>
                  <TableHead className="text-muted-foreground">허브명</TableHead>
                  <TableHead className="text-muted-foreground">주소</TableHead>
                  <TableHead className="text-muted-foreground">위도</TableHead>
                  <TableHead className="text-muted-foreground">경도</TableHead>
                  <TableHead className="text-muted-foreground">상태</TableHead>
                  <TableHead className="text-muted-foreground w-12"></TableHead>
                </TableRow>
              </TableHeader>
              <TableBody>
                {filteredHubs.map((hub, index) => (
                  <TableRow key={hub.id || `hub-${index}`} className="border-border hover:bg-muted/50">
                    <TableCell className="font-mono text-sm text-primary max-w-[120px] truncate" title={hub.id || 'N/A'}>{hub.id || '-'}</TableCell>
                    <TableCell>
                      <div className="flex items-center gap-2">
                        <div className="p-1.5 rounded bg-primary/10">
                          <MapPin className="w-3.5 h-3.5 text-primary" />
                        </div>
                        <span className="font-medium">{hub.name}</span>
                      </div>
                    </TableCell>
                    <TableCell className="text-muted-foreground max-w-xs truncate">{hub.address}</TableCell>
                    <TableCell className="font-mono text-sm">{hub.latitude?.toFixed(4) || '0.0000'}</TableCell>
                    <TableCell className="font-mono text-sm">{hub.longitude?.toFixed(4) || '0.0000'}</TableCell>
                    <TableCell>
                      <Badge
                        variant="outline"
                        className="bg-success/20 text-success border-success/30"
                      >
                        운영중
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
                ))}
              </TableBody>
            </Table>
          </CardContent>
        </Card>
      </div>
    </DashboardLayout>
  )
}
