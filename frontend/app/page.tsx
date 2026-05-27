import { DashboardLayout } from "@/components/layout/dashboard-layout"
import { StatsCard } from "@/components/dashboard/stats-card"
import { RecentOrders } from "@/components/dashboard/recent-orders"
import { HubStatus } from "@/components/dashboard/hub-status"
import { DeliveryChart } from "@/components/dashboard/delivery-chart"
import { Warehouse, Package, Truck, ShoppingCart } from "lucide-react"

export default function DashboardPage() {
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
            value={17}
            change="모든 허브 정상 운영"
            changeType="positive"
            icon={Warehouse}
            iconColor="text-primary"
          />
          <StatsCard
            title="오늘 주문"
            value={245}
            change="+12% 전일 대비"
            changeType="positive"
            icon={ShoppingCart}
            iconColor="text-info"
          />
          <StatsCard
            title="배송중"
            value={89}
            change="평균 2.5시간 소요"
            changeType="neutral"
            icon={Truck}
            iconColor="text-warning"
          />
          <StatsCard
            title="상품 수"
            value="1,234"
            change="+5 신규 등록"
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
