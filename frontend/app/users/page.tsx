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



const roleConfig = {
  MASTER: { label: "마스터 관리자", className: "bg-destructive/20 text-destructive border-destructive/30" },
  HUB_MANAGER: { label: "허브 관리자", className: "bg-info/20 text-info border-info/30" },
  DELIVERY_MANAGER: { label: "배송 담당자", className: "bg-warning/20 text-warning border-warning/30" },
  SUPPLIER_MANAGER: { label: "업체 담당자", className: "bg-success/20 text-success border-success/30" },
}

export default function UsersPage() {
  const [users, setUsers] = useState<any[]>([])
  const [loading, setLoading] = useState(true)
  const [searchTerm, setSearchTerm] = useState("")
  const [roleFilter, setRoleFilter] = useState<string>("all")

  useEffect(() => {
    async function loadUsers() {
      try {
        setLoading(true)
        const pageData = await fetchApi<any>('/users?size=50')
        setUsers(pageData.content || [])
      } catch (error) {
        console.error("Failed to fetch users:", error)
      } finally {
        setLoading(false)
      }
    }
    loadUsers()
  }, [])

  const filteredUsers = users.filter((user) => {
    const matchesSearch =
      user.username.toLowerCase().includes(searchTerm.toLowerCase()) ||
      user.nickname.toLowerCase().includes(searchTerm.toLowerCase()) ||
      user.email.toLowerCase().includes(searchTerm.toLowerCase())
    const matchesRole = roleFilter === "all" || user.role === roleFilter
    return matchesSearch && matchesRole
  })

  return (
    <DashboardLayout>
      <div className="space-y-6">
        <div className="flex items-center justify-between">
          <div>
            <h1 className="text-2xl font-bold tracking-tight">사용자 관리</h1>
            <p className="text-muted-foreground">시스템 사용자 및 권한을 관리합니다.</p>
          </div>
          <Button className="bg-primary hover:bg-primary/90">
            <Plus className="w-4 h-4 mr-2" />
            사용자 추가
          </Button>
        </div>

        <div className="grid gap-4 md:grid-cols-4">
          <Card className="bg-card border-border">
            <CardContent className="p-6">
              <div className="text-sm text-muted-foreground">전체 사용자</div>
              <div className="text-3xl font-bold mt-1">{users.length}</div>
            </CardContent>
          </Card>
          <Card className="bg-card border-border">
            <CardContent className="p-6">
              <div className="text-sm text-muted-foreground">허브 관리자</div>
              <div className="text-3xl font-bold mt-1 text-info">
                {users.filter((u) => u.role === "HUB_MANAGER").length}
              </div>
            </CardContent>
          </Card>
          <Card className="bg-card border-border">
            <CardContent className="p-6">
              <div className="text-sm text-muted-foreground">배송 담당자</div>
              <div className="text-3xl font-bold mt-1 text-warning">
                {users.filter((u) => u.role === "DELIVERY_MANAGER").length}
              </div>
            </CardContent>
          </Card>
          <Card className="bg-card border-border">
            <CardContent className="p-6">
              <div className="text-sm text-muted-foreground">업체 담당자</div>
              <div className="text-3xl font-bold mt-1 text-success">
                {users.filter((u) => u.role === "SUPPLIER_MANAGER").length}
              </div>
            </CardContent>
          </Card>
        </div>

        <Card className="bg-card border-border">
          <CardHeader className="pb-4">
            <div className="flex items-center justify-between">
              <div className="flex items-center gap-2">
                <CardTitle className="text-lg">사용자 목록</CardTitle>
                <div className="flex gap-1 ml-4">
                  {["all", "MASTER", "HUB_MANAGER", "DELIVERY_MANAGER", "SUPPLIER_MANAGER"].map((role) => (
                    <Button
                      key={role}
                      variant={roleFilter === role ? "default" : "ghost"}
                      size="sm"
                      onClick={() => setRoleFilter(role)}
                      className={roleFilter === role ? "bg-primary" : ""}
                    >
                      {role === "all" ? "전체" : roleConfig[role as keyof typeof roleConfig]?.label}
                    </Button>
                  ))}
                </div>
              </div>
              <div className="relative w-72">
                <Search className="absolute left-3 top-1/2 -translate-y-1/2 w-4 h-4 text-muted-foreground" />
                <Input
                  placeholder="이름, 아이디, 이메일 검색..."
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
                  <TableHead className="text-muted-foreground">사용자 ID</TableHead>
                  <TableHead className="text-muted-foreground">이름</TableHead>
                  <TableHead className="text-muted-foreground">이메일</TableHead>
                  <TableHead className="text-muted-foreground">권한</TableHead>
                  <TableHead className="text-muted-foreground">소속</TableHead>
                  <TableHead className="text-muted-foreground">공개</TableHead>
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
                ) : filteredUsers.length === 0 ? (
                  <TableRow>
                    <TableCell colSpan={7} className="text-center py-8 text-muted-foreground">
                      등록된 사용자가 없습니다.
                    </TableCell>
                  </TableRow>
                ) : (
                  filteredUsers.map((user, index) => (
                    <TableRow key={user.id || `usr-${index}`} className="border-border hover:bg-muted/50">
                      <TableCell className="font-mono text-sm text-primary">{user.username}</TableCell>
                      <TableCell>
                        <div className="flex items-center gap-2">
                          <Avatar className="h-8 w-8">
                            <AvatarFallback className="bg-primary/20 text-primary text-xs">
                              {user.nickname?.slice(0, 2) || "US"}
                            </AvatarFallback>
                          </Avatar>
                          <span className="font-medium">{user.nickname}</span>
                        </div>
                      </TableCell>
                      <TableCell className="text-muted-foreground">{user.email}</TableCell>
                      <TableCell>
                        <Badge
                          variant="outline"
                          className={roleConfig[user.role as keyof typeof roleConfig]?.className}
                        >
                          {roleConfig[user.role as keyof typeof roleConfig]?.label || user.role}
                        </Badge>
                      </TableCell>
                      <TableCell className="text-muted-foreground">{user.hub || "-"}</TableCell>
                      <TableCell>
                        <Badge
                          variant="outline"
                          className={
                            user.isPublic
                              ? "bg-success/20 text-success border-success/30"
                              : "bg-muted text-muted-foreground border-muted"
                          }
                        >
                          {user.isPublic ? "공개" : "비공개"}
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
                              비활성화
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
