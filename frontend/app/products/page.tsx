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
import { Plus, Search, MoreHorizontal, Pencil, Trash2, Package } from "lucide-react"
import { useState, useEffect, useCallback } from "react"
import { fetchApi } from "@/lib/api"
import { toast } from "sonner"
import { CreateProductModal } from "@/components/products/create-product-modal"

// ProductStatus: ACTIVE, INACTIVE, DELETED
const productStatusConfig = {
  ACTIVE: { label: "판매중", className: "bg-success/20 text-success border-success/30" },
  INACTIVE: { label: "판매중지", className: "bg-warning/20 text-warning border-warning/30" },
  DELETED: { label: "삭제됨", className: "bg-destructive/20 text-destructive border-destructive/30" },
}

export default function ProductsPage() {
  const [products, setProducts] = useState<any[]>([])
  const [companies, setCompanies] = useState<Record<string, string>>({})
  const [hubs, setHubs] = useState<Record<string, string>>({})
  const [loading, setLoading] = useState(true)
  const [searchTerm, setSearchTerm] = useState("")

  const loadMeta = useCallback(async () => {
    try {
      const [companyData, hubData] = await Promise.all([
        fetchApi<any>('/companies?size=100'),
        fetchApi<any>('/hubs?size=50'),
      ])
      const companyMap: Record<string, string> = {}
      ;(companyData.content || []).forEach((c: any) => { companyMap[c.companyId] = c.companyName })
      setCompanies(companyMap)

      const hubMap: Record<string, string> = {}
      ;(hubData.content || []).forEach((h: any) => { hubMap[h.hubId] = h.name })
      setHubs(hubMap)
    } catch (error) {
      console.error("Failed to fetch meta:", error)
    }
  }, [])

  const loadProducts = useCallback(async () => {
    try {
      setLoading(true)
      const pageData = await fetchApi<any>('/products?size=50')
      setProducts(pageData.content || [])
    } catch (error) {
      console.error("Failed to fetch products:", error)
      toast.error("상품 목록을 불러오지 못했습니다.")
    } finally {
      setLoading(false)
    }
  }, [])

  useEffect(() => {
    loadMeta()
    loadProducts()
  }, [loadMeta, loadProducts])

  const handleDelete = async (productId: string) => {
    if (!confirm("정말로 이 상품을 삭제하시겠습니까?")) return
    try {
      await fetchApi(`/products/${productId}`, { method: 'DELETE' })
      toast.success("상품이 삭제되었습니다.")
      loadProducts()
    } catch (error: any) {
      toast.error(error.message || "상품 삭제에 실패했습니다.")
    }
  }

  const filteredProducts = products.filter((product) => {
    const companyName = companies[product.companyId] || ""
    const hubName = hubs[product.hubId] || ""
    return (
      (product.name || "").toLowerCase().includes(searchTerm.toLowerCase()) ||
      companyName.toLowerCase().includes(searchTerm.toLowerCase()) ||
      hubName.toLowerCase().includes(searchTerm.toLowerCase())
    )
  })

  const activeCount = products.filter((p) => p.status === "ACTIVE").length
  const inactiveCount = products.filter((p) => p.status === "INACTIVE").length

  return (
    <DashboardLayout>
      <div className="space-y-6">
        <div className="flex items-center justify-between">
          <div>
            <h1 className="text-2xl font-bold tracking-tight">상품 관리</h1>
            <p className="text-muted-foreground">업체별 상품을 관리합니다.</p>
          </div>
          <CreateProductModal onSuccess={loadProducts} />
        </div>

        <div className="grid gap-4 md:grid-cols-3">
          <Card className="bg-card border-border">
            <CardContent className="p-6">
              <div className="text-sm text-muted-foreground">전체 상품</div>
              <div className="text-3xl font-bold mt-1">{products.length}</div>
            </CardContent>
          </Card>
          <Card className="bg-card border-border">
            <CardContent className="p-6">
              <div className="text-sm text-muted-foreground">판매중</div>
              <div className="text-3xl font-bold mt-1 text-success">{activeCount}</div>
            </CardContent>
          </Card>
          <Card className="bg-card border-border">
            <CardContent className="p-6">
              <div className="text-sm text-muted-foreground">판매중지</div>
              <div className="text-3xl font-bold mt-1 text-warning">{inactiveCount}</div>
            </CardContent>
          </Card>
        </div>

        <Card className="bg-card border-border">
          <CardHeader className="pb-4">
            <div className="flex items-center justify-between">
              <CardTitle className="text-lg">상품 목록</CardTitle>
              <div className="relative w-72">
                <Search className="absolute left-3 top-1/2 -translate-y-1/2 w-4 h-4 text-muted-foreground" />
                <Input
                  placeholder="상품명, 업체명 또는 허브명 검색..."
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
                  <TableHead className="text-muted-foreground">상품명</TableHead>
                  <TableHead className="text-muted-foreground">업체</TableHead>
                  <TableHead className="text-muted-foreground">관리 허브</TableHead>
                  <TableHead className="text-muted-foreground">가격</TableHead>
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
                ) : filteredProducts.length === 0 ? (
                  <TableRow>
                    <TableCell colSpan={6} className="text-center py-8 text-muted-foreground">
                      등록된 상품이 없습니다.
                    </TableCell>
                  </TableRow>
                ) : (
                  filteredProducts.map((product, index) => (
                    <TableRow key={product.productId || `prd-${index}`} className="border-border hover:bg-muted/50">
                      <TableCell>
                        <div className="flex items-center gap-2">
                          <div className="p-1.5 rounded bg-primary/10">
                            <Package className="w-3.5 h-3.5 text-primary" />
                          </div>
                          <span className="font-medium">{product.name}</span>
                        </div>
                      </TableCell>
                      <TableCell className="text-muted-foreground">
                        {companies[product.companyId] || product.companyId?.slice(0, 8) + '...' || '-'}
                      </TableCell>
                      <TableCell className="text-muted-foreground">
                        {hubs[product.hubId] || product.hubId?.slice(0, 8) + '...' || '-'}
                      </TableCell>
                      <TableCell>
                        <span className="font-mono">{product.price?.toLocaleString() || 0}</span>
                        <span className="text-muted-foreground ml-1">원</span>
                      </TableCell>
                      <TableCell>
                        <Badge
                          variant="outline"
                          className={productStatusConfig[product.status as keyof typeof productStatusConfig]?.className}
                        >
                          {productStatusConfig[product.status as keyof typeof productStatusConfig]?.label || product.status}
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
                            <DropdownMenuItem className="text-destructive cursor-pointer" onClick={() => handleDelete(product.productId)}>
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
