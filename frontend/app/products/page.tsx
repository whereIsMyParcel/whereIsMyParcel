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
import { Plus, Search, MoreHorizontal, Pencil, Trash2, Package } from "lucide-react"
import { useState, useEffect } from "react"
import { fetchApi } from "@/lib/api"



export default function ProductsPage() {
  const [products, setProducts] = useState<any[]>([])
  const [loading, setLoading] = useState(true)
  const [searchTerm, setSearchTerm] = useState("")

  useEffect(() => {
    async function loadProducts() {
      try {
        setLoading(true)
        const pageData = await fetchApi<any>('/products?size=50')
        setProducts(pageData.content || [])
      } catch (error) {
        console.error("Failed to fetch products:", error)
      } finally {
        setLoading(false)
      }
    }
    loadProducts()
  }, [])

  const filteredProducts = products.filter(
    (product) =>
      product.name.toLowerCase().includes(searchTerm.toLowerCase()) ||
      product.company.toLowerCase().includes(searchTerm.toLowerCase())
  )

  const totalStock = products.reduce((acc, p) => acc + p.stock, 0)
  const lowStockCount = products.filter((p) => p.stock < 300).length

  return (
    <DashboardLayout>
      <div className="space-y-6">
        <div className="flex items-center justify-between">
          <div>
            <h1 className="text-2xl font-bold tracking-tight">상품 관리</h1>
            <p className="text-muted-foreground">업체별 상품과 재고를 관리합니다.</p>
          </div>
          <Button className="bg-primary hover:bg-primary/90">
            <Plus className="w-4 h-4 mr-2" />
            상품 추가
          </Button>
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
              <div className="text-sm text-muted-foreground">총 재고량</div>
              <div className="text-3xl font-bold mt-1">{totalStock.toLocaleString()}</div>
            </CardContent>
          </Card>
          <Card className="bg-card border-border">
            <CardContent className="p-6">
              <div className="text-sm text-muted-foreground">재고 부족</div>
              <div className="text-3xl font-bold mt-1 text-warning">{lowStockCount}</div>
              <p className="text-xs text-muted-foreground mt-1">300개 미만</p>
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
                  placeholder="상품명 또는 업체명 검색..."
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
                  <TableHead className="text-muted-foreground">상품 ID</TableHead>
                  <TableHead className="text-muted-foreground">상품명</TableHead>
                  <TableHead className="text-muted-foreground">업체</TableHead>
                  <TableHead className="text-muted-foreground">관리 허브</TableHead>
                  <TableHead className="text-muted-foreground">재고</TableHead>
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
                    <TableRow key={product.id || `prd-${index}`} className="border-border hover:bg-muted/50">
                      <TableCell className="font-mono text-sm text-primary">{product.id}</TableCell>
                      <TableCell>
                        <div className="flex items-center gap-2">
                          <div className="p-1.5 rounded bg-primary/10">
                            <Package className="w-3.5 h-3.5 text-primary" />
                          </div>
                          <span className="font-medium">{product.name}</span>
                        </div>
                      </TableCell>
                      <TableCell className="text-muted-foreground">{product.company}</TableCell>
                      <TableCell className="text-muted-foreground">{product.hub}</TableCell>
                      <TableCell>
                        <span
                          className={
                            product.stock < 300
                              ? "text-warning font-semibold"
                              : "font-mono"
                          }
                        >
                          {product.stock?.toLocaleString() || 0}
                        </span>
                        <span className="text-muted-foreground ml-1">개</span>
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
