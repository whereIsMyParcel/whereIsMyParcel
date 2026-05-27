"use client"

import { useState, useEffect } from "react"
import { useForm } from "react-hook-form"
import { zodResolver } from "@hookform/resolvers/zod"
import * as z from "zod"
import { toast } from "sonner"
import {
  Dialog,
  DialogContent,
  DialogHeader,
  DialogTitle,
  DialogFooter,
  DialogDescription,
} from "@/components/ui/dialog"
import {
  Form,
  FormControl,
  FormField,
  FormItem,
  FormLabel,
  FormMessage,
} from "@/components/ui/form"
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from "@/components/ui/select"
import { Input } from "@/components/ui/input"
import { Button } from "@/components/ui/button"
import { fetchApi } from "@/lib/api"

const driverFormSchema = z.object({
  type: z.enum(["HUB_DELIVERY", "COMPANY_DELIVERY"], { required_error: "유형을 선택해주세요." }),
  hubId: z.string().optional(),
  slackId: z.string().min(1, "Slack ID를 입력해주세요."),
}).refine((data) => {
  if (data.type === "COMPANY_DELIVERY" && !data.hubId) {
    return false
  }
  return true
}, {
  message: "업체 담당자는 소속 허브를 선택해야 합니다.",
  path: ["hubId"],
})

type DriverFormValues = z.infer<typeof driverFormSchema>

interface CreateDriverModalProps {
  open: boolean
  onOpenChange: (open: boolean) => void
  onSuccess: () => void
}

export function CreateDriverModal({ open, onOpenChange, onSuccess }: CreateDriverModalProps) {
  const [hubs, setHubs] = useState<any[]>([])
  const [isSubmitting, setIsSubmitting] = useState(false)

  const form = useForm<DriverFormValues>({
    resolver: zodResolver(driverFormSchema),
    defaultValues: {
      type: "HUB_DELIVERY",
      slackId: "",
      hubId: "",
    },
  })

  useEffect(() => {
    if (open) {
      fetchApi<any>('/hubs?size=50').then((data) => {
        setHubs(data.content || [])
      }).catch(() => {
        toast.error("허브 목록을 불러오지 못했습니다.")
      })
      form.reset()
    }
  }, [open, form])

  const onSubmit = async (data: DriverFormValues) => {
    try {
      setIsSubmitting(true)
      await fetchApi('/delivery-managers', {
        method: 'POST',
        body: JSON.stringify({
          type: data.type,
          slackId: data.slackId,
          hubId: data.type === 'HUB_DELIVERY' ? null : (data.hubId || null),
        }),
      })
      toast.success("담당자가 성공적으로 추가되었습니다.")
      onSuccess()
      onOpenChange(false)
    } catch (error: any) {
      toast.error(error.message || "담당자 추가에 실패했습니다.")
    } finally {
      setIsSubmitting(false)
    }
  }

  const selectedType = form.watch("type")

  return (
    <Dialog open={open} onOpenChange={onOpenChange}>
      <DialogContent>
        <DialogHeader>
          <DialogTitle>배송담당자 추가</DialogTitle>
          <DialogDescription>새로운 배송 담당자를 등록합니다.</DialogDescription>
        </DialogHeader>

        <Form {...form}>
          <form onSubmit={form.handleSubmit(onSubmit)} className="space-y-4">
            <FormField
              control={form.control}
              name="type"
              render={({ field }) => (
                <FormItem>
                  <FormLabel>유형</FormLabel>
                  <Select onValueChange={field.onChange} defaultValue={field.value}>
                    <FormControl>
                      <SelectTrigger>
                        <SelectValue placeholder="유형 선택" />
                      </SelectTrigger>
                    </FormControl>
                    <SelectContent>
                      <SelectItem value="HUB_DELIVERY">허브 담당자 (허브 간 이동)</SelectItem>
                      <SelectItem value="COMPANY_DELIVERY">업체 담당자 (허브 → 업체)</SelectItem>
                    </SelectContent>
                  </Select>
                  <FormMessage />
                </FormItem>
              )}
            />

            <FormField
              control={form.control}
              name="slackId"
              render={({ field }) => (
                <FormItem>
                  <FormLabel>Slack ID</FormLabel>
                  <FormControl>
                    <Input placeholder="예: U0123456789" {...field} />
                  </FormControl>
                  <FormMessage />
                </FormItem>
              )}
            />

            {selectedType === "COMPANY_DELIVERY" && (
              <FormField
                control={form.control}
                name="hubId"
                render={({ field }) => (
                  <FormItem>
                    <FormLabel>소속 허브</FormLabel>
                    <Select onValueChange={field.onChange} value={field.value || undefined}>
                      <FormControl>
                        <SelectTrigger>
                          <SelectValue placeholder="허브 선택" />
                        </SelectTrigger>
                      </FormControl>
                      <SelectContent>
                        {hubs.map((hub) => (
                          <SelectItem key={hub.hubId} value={hub.hubId}>
                            {hub.name}
                          </SelectItem>
                        ))}
                      </SelectContent>
                    </Select>
                    <FormMessage />
                  </FormItem>
                )}
              />
            )}

            <DialogFooter>
              <Button type="button" variant="outline" onClick={() => onOpenChange(false)}>취소</Button>
              <Button type="submit" disabled={isSubmitting}>
                {isSubmitting ? "추가 중..." : "추가"}
              </Button>
            </DialogFooter>
          </form>
        </Form>
      </DialogContent>
    </Dialog>
  )
}
