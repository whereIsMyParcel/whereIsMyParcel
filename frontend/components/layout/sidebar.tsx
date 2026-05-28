"use client"

import Link from "next/link"
import { usePathname } from "next/navigation"
import { cn } from "@/lib/utils"
import {
  LayoutDashboard,
  Warehouse,
  Route,
  Truck,
  Building2,
  Package,
  ShoppingCart,
  Users,
  MessageSquare,
  Settings,
  ChevronLeft,
  ChevronRight,
  PackageCheck,
} from "lucide-react"
import { useState } from "react"
import { Button } from "@/components/ui/button"
import Image from "next/image"

const navigation = [
  { name: "대시보드", href: "/", icon: LayoutDashboard },
  { name: "허브 관리", href: "/hubs", icon: Warehouse },
  { name: "이동경로 관리", href: "/routes", icon: Route },
  { name: "배송담당자", href: "/drivers", icon: Truck },
  { name: "업체 관리", href: "/companies", icon: Building2 },
  { name: "상품 관리", href: "/products", icon: Package },
  { name: "주문 관리", href: "/orders", icon: ShoppingCart },
  { name: "배송 관리", href: "/shipments", icon: PackageCheck },
  { name: "사용자 관리", href: "/users", icon: Users },
  { name: "슬랙 메시지", href: "/slack", icon: MessageSquare },
]

export function Sidebar() {
  const pathname = usePathname()
  const [collapsed, setCollapsed] = useState(false)

  return (
    <aside
      className={cn(
        "flex flex-col bg-sidebar border-r border-sidebar-border transition-all duration-300",
        collapsed ? "w-16" : "w-64"
      )}
    >
      <div className="flex items-center justify-between h-16 px-4 border-b border-sidebar-border">
        {!collapsed && (
          <Link href="/" className="flex items-center gap-2">
            <div className="w-8 h-8 flex items-center justify-center">
              <Image src="/logo.png" alt="Sparta Logistics Logo" width={32} height={32} className="rounded-md" />
            </div>
            <span className="font-semibold text-sidebar-foreground">스파르타 물류</span>
          </Link>
        )}
        {collapsed && (
          <div className="w-8 h-8 flex items-center justify-center mx-auto">
            <Image src="/logo.png" alt="Sparta Logistics Logo" width={32} height={32} className="rounded-md" />
          </div>
        )}
        <Button
          variant="ghost"
          size="icon"
          onClick={() => setCollapsed(!collapsed)}
          className={cn("text-sidebar-foreground hover:bg-sidebar-accent", collapsed && "mx-auto mt-2")}
        >
          {collapsed ? <ChevronRight className="w-4 h-4" /> : <ChevronLeft className="w-4 h-4" />}
        </Button>
      </div>

      <nav className="flex-1 p-2 space-y-1 overflow-y-auto">
        {navigation.map((item) => {
          const isActive = pathname === item.href || (item.href !== "/" && pathname.startsWith(item.href))
          return (
            <Link
              key={item.name}
              href={item.href}
              className={cn(
                "flex items-center gap-3 px-3 py-2.5 rounded-lg text-sm font-medium transition-colors",
                isActive
                  ? "bg-sidebar-accent text-sidebar-primary"
                  : "text-sidebar-foreground hover:bg-sidebar-accent hover:text-sidebar-accent-foreground"
              )}
            >
              <item.icon className={cn("w-5 h-5 shrink-0", isActive && "text-sidebar-primary")} />
              {!collapsed && <span>{item.name}</span>}
            </Link>
          )
        })}
      </nav>

      <div className="p-2 border-t border-sidebar-border">
        <Link
          href="/settings"
          className={cn(
            "flex items-center gap-3 px-3 py-2.5 rounded-lg text-sm font-medium transition-colors",
            "text-sidebar-foreground hover:bg-sidebar-accent hover:text-sidebar-accent-foreground"
          )}
        >
          <Settings className="w-5 h-5 shrink-0" />
          {!collapsed && <span>설정</span>}
        </Link>
      </div>
    </aside>
  )
}
