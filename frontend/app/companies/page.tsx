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
import { Plus, Search, MoreHorizontal, Pencil, Trash2, Building2 } from "lucide-react"
import { useState } from "react"

const companies = [
  { id: "CMP-001", name: "경기 건조식품", type: "supplier", hub: "경기 남부 센터", address: "경기도 이천시 부발읍 경충대로 1234", status: "active" },
  { id: "CMP-002", name: "서울 플라스틱", type: "supplier", hub: "서울특별시 센터", address: "서울시 송파구 문정동 456", status: "active" },
  { id: "CMP-003", name: "인천 전자", type: "supplier", hub: "인천광역시 센터", address: "인천시 남동구 논현동 789", status: "active" },
  { id: "CMP-004", name: "대전 식품", type: "supplier", hub: "대전광역시 센터", address: "대전시 유성구 관평동 321", status: "active" },
  { id: "CMP-005", name: "부산 수산물 도매", type: "receiver", hub: "부산광역시 센터", address: "부산시 동구 초량동 567", status: "active" },
  { id: "CMP-006", name: "대구 제조업체", type: "receiver", hub: "대구광역시 센터", address: "대구시 북구 산격동 890", status: "inactive" },
  { id: "CMP-007", name: "광주 조립업체", type: "receiver", hub: "광주광역시 센터", address: "광주시 서구 치평동 234", status: "active" },
  { id: "CMP-008", name: "울산 가공업체", type: "receiver", hub: "울산광역시 센터", address: "울산시 남구 삼산동 678", status: "active" },
  { id: "CMP-009", name: "경남 의류업체", type: "receiver", hub: "경상남도 센터", address: "창원시 의창구 팔용동 901", status: "active" },
  { id: "CMP-010", name: "전북 화장품", type: "receiver", hub: "전북특별자치도 센터", address: "전주시 덕진구 금암동 345", status: "active" },
]

export default function CompaniesPage() {
  const [searchTerm, setSearchTerm] = useState("")
  const [typeFilter, setTypeFilter] = useState<"all" | "supplier" | "receiver">("all")

  const filteredCompanies = companies.filter((company) => {
    const matchesSearch =
      company.name.toLowerCase().includes(searchTerm.toLowerCase()) ||
      company.address.toLowerCase().includes(searchTerm.toLowerCase())
    const matchesType = typeFilter === "all" || company.type === typeFilter
    return matchesSearch && matchesType
  })

  const supplierCount = companies.filter((c) => c.type === "supplier").length
  const receiverCount = companies.filter((c) => c.type === "receiver").length

  return (
    <DashboardLayout>
      <div className="space-y-6">
        <div className="flex items-center justify-between">
          <div>
            <h1 className="text-2xl font-bold tracking-tight">업체 관리</h1>
            <p className="text-muted-foreground">생산업체와 수령업체를 관리합니다.</p>
          </div>
          <Button className="bg-primary hover:bg-primary/90">
            <Plus className="w-4 h-4 mr-2" />
            업체 추가
          </Button>
        </div>

        <div className="grid gap-4 md:grid-cols-3">
          <Card className="bg-card border-border">
            <CardContent className="p-6">
              <div className="text-sm text-muted-foreground">전체 업체</div>
              <div className="text-3xl font-bold mt-1">{companies.length}</div>
            </CardContent>
          </Card>
          <Card className="bg-card border-border">
            <CardContent className="p-6">
              <div className="text-sm text-muted-foreground">생산업체</div>
              <div className="text-3xl font-bold mt-1 text-info">{supplierCount}</div>
            </CardContent>
          </Card>
          <Card className="bg-card border-border">
            <CardContent className="p-6">
              <div className="text-sm text-muted-foreground">수령업체</div>
              <div className="text-3xl font-bold mt-1 text-warning">{receiverCount}</div>
            </CardContent>
          </Card>
        </div>

        <Card className="bg-card border-border">
          <CardHeader className="pb-4">
            <div className="flex items-center justify-between">
              <div className="flex items-center gap-2">
                <CardTitle className="text-lg">업체 목록</CardTitle>
                <div className="flex gap-1 ml-4">
                  <Button
                    variant={typeFilter === "all" ? "default" : "ghost"}
                    size="sm"
                    onClick={() => setTypeFilter("all")}
                    className={typeFilter === "all" ? "bg-primary" : ""}
                  >
                    전체
                  </Button>
                  <Button
                    variant={typeFilter === "supplier" ? "default" : "ghost"}
                    size="sm"
                    onClick={() => setTypeFilter("supplier")}
                    className={typeFilter === "supplier" ? "bg-primary" : ""}
                  >
                    생산업체
                  </Button>
                  <Button
                    variant={typeFilter === "receiver" ? "default" : "ghost"}
                    size="sm"
                    onClick={() => setTypeFilter("receiver")}
                    className={typeFilter === "receiver" ? "bg-primary" : ""}
                  >
                    수령업체
                  </Button>
                </div>
              </div>
              <div className="relative w-72">
                <Search className="absolute left-3 top-1/2 -translate-y-1/2 w-4 h-4 text-muted-foreground" />
                <Input
                  placeholder="업체명 또는 주소 검색..."
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
                  <TableHead className="text-muted-foreground">업체 ID</TableHead>
                  <TableHead className="text-muted-foreground">업체명</TableHead>
                  <TableHead className="text-muted-foreground">유형</TableHead>
                  <TableHead className="text-muted-foreground">관리 허브</TableHead>
                  <TableHead className="text-muted-foreground">주소</TableHead>
                  <TableHead className="text-muted-foreground">상태</TableHead>
                  <TableHead className="text-muted-foreground w-12"></TableHead>
                </TableRow>
              </TableHeader>
              <TableBody>
                {filteredCompanies.map((company) => (
                  <TableRow key={company.id} className="border-border hover:bg-muted/50">
                    <TableCell className="font-mono text-sm text-primary">{company.id}</TableCell>
                    <TableCell>
                      <div className="flex items-center gap-2">
                        <div className="p-1.5 rounded bg-primary/10">
                          <Building2 className="w-3.5 h-3.5 text-primary" />
                        </div>
                        <span className="font-medium">{company.name}</span>
                      </div>
                    </TableCell>
                    <TableCell>
                      <Badge
                        variant="outline"
                        className={
                          company.type === "supplier"
                            ? "bg-info/20 text-info border-info/30"
                            : "bg-warning/20 text-warning border-warning/30"
                        }
                      >
                        {company.type === "supplier" ? "생산업체" : "수령업체"}
                      </Badge>
                    </TableCell>
                    <TableCell className="text-muted-foreground">{company.hub}</TableCell>
                    <TableCell className="text-muted-foreground max-w-xs truncate">{company.address}</TableCell>
                    <TableCell>
                      <Badge
                        variant="outline"
                        className={
                          company.status === "active"
                            ? "bg-success/20 text-success border-success/30"
                            : "bg-muted text-muted-foreground border-muted"
                        }
                      >
                        {company.status === "active" ? "활성" : "비활성"}
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
