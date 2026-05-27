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
import { Search, MoreHorizontal, CheckCircle, XCircle, Trash2 } from "lucide-react"
import { useState, useEffect, useCallback } from "react"
import { fetchApi } from "@/lib/api"
import { toast } from "sonner"
import { CreateUserModal } from "@/components/users/create-user-modal"

// UserRole: MASTER, HUB_MANAGER, DELIVERY_MANAGER, COMPANY_MANAGER
const roleConfig: Record<string, { label: string; className: string }> = {
  MASTER: { label: "마스터 관리자", className: "bg-destructive/20 text-destructive border-destructive/30" },
  HUB_MANAGER: { label: "허브 관리자", className: "bg-info/20 text-info border-info/30" },
  DELIVERY_MANAGER: { label: "배송 담당자", className: "bg-warning/20 text-warning border-warning/30" },
  COMPANY_MANAGER: { label: "업체 담당자", className: "bg-success/20 text-success border-success/30" },
}

// UserStatus: PENDING, APPROVED, REJECTED
const statusConfig: Record<string, { label: string; className: string }> = {
  PENDING: { label: "승인 대기", className: "bg-warning/20 text-warning border-warning/30" },
  APPROVED: { label: "승인됨", className: "bg-success/20 text-success border-success/30" },
  REJECTED: { label: "거절됨", className: "bg-destructive/20 text-destructive border-destructive/30" },
}

export default function UsersPage() {
  const [users, setUsers] = useState<any[]>([])
  const [hubs, setHubs] = useState<Record<string, string>>({})
  const [loading, setLoading] = useState(true)
  const [searchTerm, setSearchTerm] = useState("")
  const [roleFilter, setRoleFilter] = useState<string>("all")
  const [statusFilter, setStatusFilter] = useState<string>("all")

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

  const loadUsers = useCallback(async () => {
    try {
      setLoading(true)
      const pageData = await fetchApi<any>('/users?size=50')
      setUsers(pageData.content || [])
    } catch (error) {
      console.error("Failed to fetch users:", error)
      toast.error("사용자 목록을 불러오지 못했습니다.")
    } finally {
      setLoading(false)
    }
  }, [])

  useEffect(() => {
    loadHubs()
    loadUsers()
  }, [loadHubs, loadUsers])

  const handleApprove = async (userId: string) => {
    try {
      await fetchApi(`/users/${userId}/approve`, { method: 'PATCH' })
      toast.success("사용자가 승인되었습니다.")
      loadUsers()
    } catch (error: any) {
      toast.error(error.message || "승인에 실패했습니다.")
    }
  }

  const handleReject = async (userId: string) => {
    if (!confirm("이 사용자의 가입 신청을 거절하시겠습니까?")) return
    try {
      await fetchApi(`/users/${userId}/reject`, { method: 'PATCH' })
      toast.success("사용자 가입 신청이 거절되었습니다.")
      loadUsers()
    } catch (error: any) {
      toast.error(error.message || "거절에 실패했습니다.")
    }
  }

  const handleDelete = async (userId: string) => {
    if (!confirm("정말 이 사용자를 삭제(비활성화)하시겠습니까?")) return
    try {
      await fetchApi(`/users/${userId}`, { method: 'DELETE' })
      toast.success("사용자가 삭제(비활성화)되었습니다.")
      loadUsers()
    } catch (error: any) {
      toast.error(error.message || "삭제에 실패했습니다.")
    }
  }

  const filteredUsers = users.filter((user) => {
    const matchesSearch =
      (user.username || "").toLowerCase().includes(searchTerm.toLowerCase()) ||
      (user.name || "").toLowerCase().includes(searchTerm.toLowerCase()) ||
      (user.email || "").toLowerCase().includes(searchTerm.toLowerCase())
    const matchesRole = roleFilter === "all" || user.role === roleFilter
    const matchesStatus = statusFilter === "all" || user.status === statusFilter
    return matchesSearch && matchesRole && matchesStatus
  })

  const pendingCount = users.filter((u) => u.status === "PENDING").length

  return (
    <DashboardLayout>
      <div className="space-y-6">
        <div className="flex items-center justify-between">
          <div>
            <h1 className="text-2xl font-bold tracking-tight">사용자 관리</h1>
            <p className="text-muted-foreground">시스템 사용자 및 권한을 관리합니다.</p>
          </div>
          <CreateUserModal onSuccess={loadUsers} />
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
              <div className="text-sm text-muted-foreground">승인 대기</div>
              <div className="text-3xl font-bold mt-1 text-warning">{pendingCount}</div>
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
              <div className="text-sm text-muted-foreground">업체 담당자</div>
              <div className="text-3xl font-bold mt-1 text-success">
                {users.filter((u) => u.role === "COMPANY_MANAGER").length}
              </div>
            </CardContent>
          </Card>
        </div>

        <Card className="bg-card border-border">
          <CardHeader className="pb-4">
            <div className="flex flex-col gap-3">
              <div className="flex items-center justify-between">
                <div className="flex items-center gap-2">
                  <CardTitle className="text-lg">사용자 목록</CardTitle>
                  <div className="flex gap-1 ml-4 flex-wrap">
                    {["all", "MASTER", "HUB_MANAGER", "DELIVERY_MANAGER", "COMPANY_MANAGER"].map((role) => (
                      <Button
                        key={role}
                        variant={roleFilter === role ? "default" : "ghost"}
                        size="sm"
                        onClick={() => setRoleFilter(role)}
                        className={roleFilter === role ? "bg-primary" : ""}
                      >
                        {role === "all" ? "전체" : roleConfig[role]?.label}
                      </Button>
                    ))}
                  </div>
                </div>
                <div className="relative w-72">
                  <Search className="absolute left-3 top-1/2 -translate-y-1/2 w-4 h-4 text-muted-foreground" />
                  <Input
                    placeholder="아이디, 이름, 이메일 검색..."
                    className="pl-10 bg-muted border-0"
                    value={searchTerm}
                    onChange={(e) => setSearchTerm(e.target.value)}
                  />
                </div>
              </div>
              <div className="flex gap-1">
                {["all", "PENDING", "APPROVED", "REJECTED"].map((status) => (
                  <Button
                    key={status}
                    variant={statusFilter === status ? "secondary" : "ghost"}
                    size="sm"
                    onClick={() => setStatusFilter(status)}
                  >
                    {status === "all" ? "상태 전체" : statusConfig[status]?.label || status}
                    {status === "PENDING" && pendingCount > 0 && (
                      <span className="ml-1.5 bg-warning text-warning-foreground text-xs px-1.5 py-0.5 rounded-full">
                        {pendingCount}
                      </span>
                    )}
                  </Button>
                ))}
              </div>
            </div>
          </CardHeader>
          <CardContent>
            <Table>
              <TableHeader>
                <TableRow className="border-border hover:bg-transparent">
                  <TableHead className="text-muted-foreground">사용자</TableHead>
                  <TableHead className="text-muted-foreground">이메일</TableHead>
                  <TableHead className="text-muted-foreground">권한</TableHead>
                  <TableHead className="text-muted-foreground">소속</TableHead>
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
                ) : filteredUsers.length === 0 ? (
                  <TableRow>
                    <TableCell colSpan={6} className="text-center py-8 text-muted-foreground">
                      등록된 사용자가 없습니다.
                    </TableCell>
                  </TableRow>
                ) : (
                  filteredUsers.map((user, index) => (
                    <TableRow key={user.userId || `usr-${index}`} className="border-border hover:bg-muted/50">
                      <TableCell>
                        <div className="flex items-center gap-2">
                          <Avatar className="h-8 w-8">
                            <AvatarFallback className="bg-primary/20 text-primary text-xs">
                              {(user.name || user.username || "??").slice(0, 2).toUpperCase()}
                            </AvatarFallback>
                          </Avatar>
                          <div>
                            <p className="font-medium">{user.name || "-"}</p>
                            <p className="text-xs text-muted-foreground font-mono">{user.username}</p>
                          </div>
                        </div>
                      </TableCell>
                      <TableCell className="text-muted-foreground">{user.email}</TableCell>
                      <TableCell>
                        <Badge variant="outline" className={roleConfig[user.role]?.className}>
                          {roleConfig[user.role]?.label || user.role}
                        </Badge>
                      </TableCell>
                      <TableCell className="text-muted-foreground text-sm">
                        {user.hubId ? (hubs[user.hubId] || user.hubId.slice(0, 8) + '...') : "-"}
                      </TableCell>
                      <TableCell>
                        <Badge variant="outline" className={statusConfig[user.status]?.className}>
                          {statusConfig[user.status]?.label || user.status}
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
                            {user.status === "PENDING" && (
                              <>
                                <DropdownMenuItem
                                  className="text-success cursor-pointer"
                                  onClick={() => handleApprove(user.userId)}
                                >
                                  <CheckCircle className="w-4 h-4 mr-2" />
                                  승인
                                </DropdownMenuItem>
                                <DropdownMenuItem
                                  className="text-destructive cursor-pointer"
                                  onClick={() => handleReject(user.userId)}
                                >
                                  <XCircle className="w-4 h-4 mr-2" />
                                  거절
                                </DropdownMenuItem>
                              </>
                            )}
                            <DropdownMenuItem
                              className="text-destructive cursor-pointer"
                              onClick={() => handleDelete(user.userId)}
                            >
                              <Trash2 className="w-4 h-4 mr-2" />
                              삭제 (비활성화)
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
