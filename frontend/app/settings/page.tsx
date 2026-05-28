"use client"

import { DashboardLayout } from "@/components/layout/dashboard-layout"
import { Button } from "@/components/ui/button"
import { Input } from "@/components/ui/input"
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from "@/components/ui/card"
import { Label } from "@/components/ui/label"
import { Switch } from "@/components/ui/switch"
import { Separator } from "@/components/ui/separator"
import { Save } from "lucide-react"

export default function SettingsPage() {
  return (
    <DashboardLayout>
      <div className="space-y-6 max-w-4xl">
        <div>
          <h1 className="text-2xl font-bold tracking-tight">설정</h1>
          <p className="text-muted-foreground">시스템 설정을 관리합니다.</p>
        </div>

        <div className="space-y-6">
          <Card className="bg-card border-border">
            <CardHeader>
              <CardTitle>일반 설정</CardTitle>
              <CardDescription>기본 시스템 설정을 구성합니다.</CardDescription>
            </CardHeader>
            <CardContent className="space-y-6">
              <div className="space-y-2">
                <Label htmlFor="company-name">회사명</Label>
                <Input id="company-name" defaultValue="스파르타 물류" className="bg-muted border-0" />
              </div>
              <div className="space-y-2">
                <Label htmlFor="admin-email">관리자 이메일</Label>
                <Input id="admin-email" defaultValue="admin@sparta.com" className="bg-muted border-0" />
              </div>
              <div className="space-y-2">
                <Label htmlFor="timezone">시간대</Label>
                <Input id="timezone" defaultValue="Asia/Seoul (KST)" className="bg-muted border-0" disabled />
              </div>
            </CardContent>
          </Card>

          <Card className="bg-card border-border">
            <CardHeader>
              <CardTitle>알림 설정</CardTitle>
              <CardDescription>알림 및 슬랙 연동을 설정합니다.</CardDescription>
            </CardHeader>
            <CardContent className="space-y-6">
              <div className="flex items-center justify-between">
                <div className="space-y-0.5">
                  <Label>주문 생성 알림</Label>
                  <p className="text-sm text-muted-foreground">새 주문이 생성되면 슬랙으로 알림을 받습니다.</p>
                </div>
                <Switch defaultChecked />
              </div>
              <Separator className="bg-border" />
              <div className="flex items-center justify-between">
                <div className="space-y-0.5">
                  <Label>배송 상태 변경 알림</Label>
                  <p className="text-sm text-muted-foreground">배송 상태가 변경되면 알림을 받습니다.</p>
                </div>
                <Switch defaultChecked />
              </div>
              <Separator className="bg-border" />
              <div className="flex items-center justify-between">
                <div className="space-y-0.5">
                  <Label>재고 부족 알림</Label>
                  <p className="text-sm text-muted-foreground">재고가 임계값 이하로 떨어지면 알림을 받습니다.</p>
                </div>
                <Switch defaultChecked />
              </div>
              <Separator className="bg-border" />
              <div className="space-y-2">
                <Label htmlFor="slack-webhook">Slack Webhook URL</Label>
                <Input
                  id="slack-webhook"
                  placeholder="https://hooks.slack.com/services/..."
                  className="bg-muted border-0 font-mono text-sm"
                />
              </div>
            </CardContent>
          </Card>

          <Card className="bg-card border-border">
            <CardHeader>
              <CardTitle>보안 설정</CardTitle>
              <CardDescription>인증 및 보안 관련 설정을 구성합니다.</CardDescription>
            </CardHeader>
            <CardContent className="space-y-6">
              <div className="flex items-center justify-between">
                <div className="space-y-0.5">
                  <Label>2단계 인증</Label>
                  <p className="text-sm text-muted-foreground">관리자 계정에 2단계 인증을 활성화합니다.</p>
                </div>
                <Switch />
              </div>
              <Separator className="bg-border" />
              <div className="flex items-center justify-between">
                <div className="space-y-0.5">
                  <Label>세션 타임아웃</Label>
                  <p className="text-sm text-muted-foreground">비활성 상태에서 자동 로그아웃됩니다.</p>
                </div>
                <Switch defaultChecked />
              </div>
              <Separator className="bg-border" />
              <div className="space-y-2">
                <Label htmlFor="session-timeout">세션 타임아웃 시간 (분)</Label>
                <Input id="session-timeout" type="number" defaultValue="30" className="bg-muted border-0 w-32" />
              </div>
            </CardContent>
          </Card>

          <Card className="bg-card border-border">
            <CardHeader>
              <CardTitle>API 설정</CardTitle>
              <CardDescription>외부 시스템 연동을 위한 API 설정입니다.</CardDescription>
            </CardHeader>
            <CardContent className="space-y-6">
              <div className="space-y-2">
                <Label htmlFor="api-gateway">API Gateway URL</Label>
                <Input
                  id="api-gateway"
                  defaultValue="http://localhost:8080"
                  className="bg-muted border-0 font-mono text-sm"
                />
              </div>
              <div className="space-y-2">
                <Label htmlFor="eureka-server">Eureka Server URL</Label>
                <Input
                  id="eureka-server"
                  defaultValue="http://localhost:8761"
                  className="bg-muted border-0 font-mono text-sm"
                />
              </div>
              <div className="space-y-2">
                <Label htmlFor="keycloak">Keycloak URL</Label>
                <Input
                  id="keycloak"
                  defaultValue="http://localhost:8180"
                  className="bg-muted border-0 font-mono text-sm"
                />
              </div>
            </CardContent>
          </Card>

          <div className="flex justify-end">
            <Button className="bg-primary hover:bg-primary/90">
              <Save className="w-4 h-4 mr-2" />
              설정 저장
            </Button>
          </div>
        </div>
      </div>
    </DashboardLayout>
  )
}
