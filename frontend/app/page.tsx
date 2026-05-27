"use client"

import { DashboardLayout } from "@/components/layout/dashboard-layout"
import { StatsCard } from "@/components/dashboard/stats-card"
import { RecentOrders } from "@/components/dashboard/recent-orders"
import { HubStatus } from "@/components/dashboard/hub-status"
import { DeliveryChart } from "@/components/dashboard/delivery-chart"
import { Warehouse, Package, Truck, ShoppingCart } from "lucide-react"
import { useState, useEffect } from "react"
import { fetchApi } from "@/lib/api"

export default function DashboardPage() {
  const [stats, setStats] = useState({
    hubCount: 0,
    orderCount: 0,
    shipmentCount: 0,
    productCount: 0,
  })

  useEffect(() => {
    Promise.all([
      fetchApi<any>('/hubs?size=10').catch(() => ({ totalElements: 0 })),
      fetchApi<any>('/orders?size=1').catch(() => ({ totalElements: 0 })),
      fetchApi<any>('/shipments?size=1').catch(() => ({ totalElements: 0 })),
      fetchApi<any>('/products?size=1').catch(() => ({ totalElements: 0 })),
    ]).then(([hubs, orders, shipments, products]) => {
      setStats({
        hubCount: hubs.totalElements ?? hubs.content?.length ?? 0,
        orderCount: orders.totalElements ?? orders.content?.length ?? 0,
        shipmentCount: shipments.totalElements ?? shipments.content?.length ?? 0,
        productCount: products.totalElements ?? products.content?.length ?? 0,
      })
    }).catch(console.error)
  }, [])

  return (
    <DashboardLayout>
      <div className="space-y-6">
        <div>
          <h1 className="text-2xl font-bold tracking-tight">대시보드</h1>
          <p className="text-muted-foreground">스파르타 물류 시스템 현황을 한눈에 확인하세요.</p>
        </div>

        <div className="grid gap-4 md:grid-cols-2 lg:grid-cols-4">
          <StatsCard
            title="전체 허브"
            value={stats.hubCount}
            change="허브 운영 현황"
            changeType="positive"
            icon={Warehouse}
            iconColor="text-primary"
          />
          <StatsCard
            title="전체 주문"
            value={stats.orderCount}
            change="주문 관리"
            changeType="positive"
            icon={ShoppingCart}
            iconColor="text-info"
          />
          <StatsCard
            title="배송 건수"
            value={stats.shipmentCount}
            change="배송 현황"
            changeType="neutral"
            icon={Truck}
            iconColor="text-warning"
          />
          <StatsCard
            title="상품 수"
            value={stats.productCount}
            change="상품 관리"
            changeType="positive"
            icon={Package}
            iconColor="text-success"
          />
        </div>

        <div className="grid gap-6 lg:grid-cols-2">
          <DeliveryChart />
          <RecentOrders />
        </div>

        <div className="grid gap-6 lg:grid-cols-1">
          <HubStatus />
        </div>
      </div>
    </DashboardLayout>
  )
}
