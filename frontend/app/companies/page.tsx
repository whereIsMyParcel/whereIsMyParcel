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
import { useState, useEffect, useCallback } from "react"
import { fetchApi } from "@/lib/api"
import { CreateCompanyModal } from "@/components/companies/create-company-modal"
import { toast } from "sonner"

// CompanyType: SUPPLIER (생산업체), RECEIVER (수령업체)
const companyTypeConfig = {
  SUPPLIER: { label: "생산업체", className: "bg-info/20 text-info border-info/30" },
  RECEIVER: { label: "수령업체", className: "bg-warning/20 text-warning border-warning/30" },
}

export default function CompaniesPage() {
  const [companies, setCompanies] = useState<any[]>([])
  const [hubs, setHubs] = useState<Record<string, string>>({})
  const [loading, setLoading] = useState(true)
  const [searchTerm, setSearchTerm] = useState("")
  const [typeFilter, setTypeFilter] = useState<"all" | "SUPPLIER" | "RECEIVER">("all")

  const loadHubs = useCallback(async () => {
    try {
      const pageData = await fetchApi<any>('/hubs?size=50')
      const hubMap: Record<string, string> = {}
      ;(pageData?.content || []).forEach((hub: any) => {
        hubMap[hub.hubId] = hub.name
      })
      setHubs(hubMap)
    } catch (error) {
      console.error("Failed to fetch hubs:", error)
    }
  }, [])

  const loadCompanies = useCallback(async () => {
    try {
      setLoading(true)
      const pageData = await fetchApi<any>('/companies?size=50')
      setCompanies(pageData?.content || [])
    } catch (error) {
      console.error("Failed to fetch companies:", error)
      toast.error("업체 목록을 불러오지 못했습니다.")
    } finally {
      setLoading(false)
    }
  }, [])

  useEffect(() => {
    loadHubs()
    loadCompanies()
  }, [loadHubs, loadCompanies])

  const handleDelete = async (companyId: string) => {
    if (!confirm("정말 이 업체를 삭제하시겠습니까?")) return
    try {
      await fetchApi(`/companies/${companyId}`, { method: 'DELETE' })
      toast.success("업체가 삭제되었습니다.")
      loadCompanies()
    } catch (error: any) {
      toast.error(error.message || "업체 삭제에 실패했습니다.")
    }
  }

  const filteredCompanies = companies.filter((company) => {
    const matchesSearch =
      (company.companyName || "").toLowerCase().includes(searchTerm.toLowerCase()) ||
      (company.address || "").toLowerCase().includes(searchTerm.toLowerCase())
    const matchesType = typeFilter === "all" || company.companyType === typeFilter
    return matchesSearch && matchesType
  })

  const supplierCount = companies.filter((c) => c.companyType === "SUPPLIER").length
  const receiverCount = companies.filter((c) => c.companyType === "RECEIVER").length

  return (
    <DashboardLayout>
      <div className="space-y-6">
        <div className="flex items-center justify-between">
          <div>
            <h1 className="text-2xl font-bold tracking-tight">업체 관리</h1>
            <p className="text-muted-foreground">생산업체와 수령업체를 관리합니다.</p>
          </div>
          <CreateCompanyModal onSuccess={loadCompanies} />
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
                  {(["all", "SUPPLIER", "RECEIVER"] as const).map((type) => (
                    <Button
                      key={type}
                      variant={typeFilter === type ? "default" : "ghost"}
                      size="sm"
                      onClick={() => setTypeFilter(type)}
                      className={typeFilter === type ? "bg-primary" : ""}
                    >
                      {type === "all" ? "전체" : companyTypeConfig[type]?.label}
                    </Button>
                  ))}
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
                  <TableHead className="text-muted-foreground">업체명</TableHead>
                  <TableHead className="text-muted-foreground">유형</TableHead>
                  <TableHead className="text-muted-foreground">관리 허브</TableHead>
                  <TableHead className="text-muted-foreground">담당자</TableHead>
                  <TableHead className="text-muted-foreground">연락처</TableHead>
                  <TableHead className="text-muted-foreground">주소</TableHead>
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
                ) : filteredCompanies.length === 0 ? (
                  <TableRow>
                    <TableCell colSpan={7} className="text-center py-8 text-muted-foreground">
                      등록된 업체가 없습니다.
                    </TableCell>
                  </TableRow>
                ) : (
                  filteredCompanies.map((company, index) => (
                    <TableRow key={company.companyId || `cmp-${index}`} className="border-border hover:bg-muted/50">
                      <TableCell>
                        <div className="flex items-center gap-2">
                          <div className="p-1.5 rounded bg-primary/10">
                            <Building2 className="w-3.5 h-3.5 text-primary" />
                          </div>
                          <span className="font-medium">{company.companyName}</span>
                        </div>
                      </TableCell>
                      <TableCell>
                        <Badge
                          variant="outline"
                          className={companyTypeConfig[company.companyType as keyof typeof companyTypeConfig]?.className}
                        >
                          {companyTypeConfig[company.companyType as keyof typeof companyTypeConfig]?.label || company.companyType}
                        </Badge>
                      </TableCell>
                      <TableCell className="text-muted-foreground text-sm">
                        {hubs[company.hubId] || company.hubId?.slice(0, 8) + '...' || '-'}
                      </TableCell>
                      <TableCell className="text-muted-foreground">{company.managerName || '-'}</TableCell>
                      <TableCell className="text-muted-foreground text-sm">{company.managerPhone || '-'}</TableCell>
                      <TableCell className="text-muted-foreground max-w-xs truncate">{company.address}</TableCell>
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
                            <DropdownMenuItem className="text-destructive cursor-pointer" onClick={() => handleDelete(company.companyId)}>
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
