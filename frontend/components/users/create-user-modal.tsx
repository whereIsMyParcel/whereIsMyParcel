"use client"

import { useState, useEffect } from "react"
import { useForm } from "react-hook-form"
import { zodResolver } from "@hookform/resolvers/zod"
import * as z from "zod"
import { toast } from "sonner"
import { Plus } from "lucide-react"

import { fetchApi } from "@/lib/api"
import { Button } from "@/components/ui/button"
import { Input } from "@/components/ui/input"
import {
  Dialog,
  DialogContent,
  DialogDescription,
  DialogFooter,
  DialogHeader,
  DialogTitle,
  DialogTrigger,
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

const formSchema = z.object({
  username: z
    .string()
    .min(4, "아이디는 최소 4자 이상이어야 합니다.")
    .max(10, "아이디는 최대 10자까지 입력 가능합니다.")
    .regex(/^[a-z0-9]+$/, "아이디는 알파벳 소문자와 숫자만 사용 가능합니다."),
  name: z.string().min(1, "이름을 입력해주세요.").max(50),
  email: z.string().email("올바른 이메일 형식을 입력해주세요."),
  password: z
    .string()
    .min(8, "비밀번호는 최소 8자 이상이어야 합니다.")
    .max(15, "비밀번호는 최대 15자까지 입력 가능합니다.")
    .regex(
      /^(?=.*[a-z])(?=.*[A-Z])(?=.*\d)(?=.*[!@#$%^&*()_+\-=\[\]{};':"\\|,.<>\/?]).{8,15}$/,
      "비밀번호는 대소문자, 숫자, 특수문자를 포함해야 합니다."
    ),
  phone: z
    .string()
    .regex(/^\d{2,3}-\d{3,4}-\d{4}$/, "전화번호 형식이 올바르지 않습니다. (예: 010-1234-5678)")
    .optional()
    .or(z.literal("")),
  slackId: z.string().min(1, "Slack ID를 입력해주세요."),
  role: z.enum(["MASTER", "HUB_MANAGER", "DELIVERY_MANAGER", "COMPANY_MANAGER"], {
    required_error: "권한을 선택해주세요.",
  }),
  hubId: z.string().uuid().optional().or(z.literal("")),
  companyId: z.string().uuid().optional().or(z.literal("")),
})

const roleOptions = [
  { value: "MASTER", label: "마스터 관리자" },
  { value: "HUB_MANAGER", label: "허브 관리자" },
  { value: "DELIVERY_MANAGER", label: "배송 담당자" },
  { value: "COMPANY_MANAGER", label: "업체 담당자" },
]

export function CreateUserModal({ onSuccess }: { onSuccess: () => void }) {
  const [open, setOpen] = useState(false)
  const [isSubmitting, setIsSubmitting] = useState(false)
  const [hubs, setHubs] = useState<any[]>([])
  const [companies, setCompanies] = useState<any[]>([])

  useEffect(() => {
    if (open) {
      Promise.all([
        fetchApi<any>('/hubs?size=50'),
        fetchApi<any>('/companies?size=100'),
      ]).then(([hubData, companyData]) => {
        setHubs(hubData.content || [])
        setCompanies(companyData.content || [])
      }).catch(console.error)
    }
  }, [open])

  const form = useForm<z.infer<typeof formSchema>>({
    resolver: zodResolver(formSchema),
    defaultValues: {
      username: "",
      name: "",
      email: "",
      password: "",
      phone: "",
      slackId: "",
      role: "COMPANY_MANAGER",
      hubId: "",
      companyId: "",
    },
  })

  const selectedRole = form.watch("role")

  async function onSubmit(values: z.infer<typeof formSchema>) {
    try {
      setIsSubmitting(true)
      const payload: any = {
        username: values.username,
        name: values.name,
        email: values.email,
        password: values.password,
        slackId: values.slackId,
        role: values.role,
      }
      if (values.phone) payload.phone = values.phone
      if (values.hubId) payload.hubId = values.hubId
      if (values.companyId) payload.companyId = values.companyId

      await fetchApi("/auth/signup", {
        method: "POST",
        body: JSON.stringify(payload),
      })
      toast.success("사용자 가입 신청이 완료되었습니다. 관리자 승인 후 로그인 가능합니다.")
      form.reset()
      setOpen(false)
      onSuccess()
    } catch (error: any) {
      toast.error(error.message || "사용자 추가에 실패했습니다.")
    } finally {
      setIsSubmitting(false)
    }
  }

  return (
    <Dialog open={open} onOpenChange={(val) => {
      setOpen(val)
      if (!val) form.reset()
    }}>
      <DialogTrigger asChild>
        <Button className="bg-primary hover:bg-primary/90">
          <Plus className="w-4 h-4 mr-2" />
          사용자 추가
        </Button>
      </DialogTrigger>
      <DialogContent className="sm:max-w-[560px] max-h-[90vh] overflow-y-auto">
        <DialogHeader>
          <DialogTitle>사용자 추가</DialogTitle>
          <DialogDescription>
            새 사용자를 등록합니다. 가입 후 관리자 승인이 필요합니다.
          </DialogDescription>
        </DialogHeader>
        <Form {...form}>
          <form onSubmit={form.handleSubmit(onSubmit)} className="space-y-4 py-2">
            <div className="grid grid-cols-2 gap-4">
              <FormField
                control={form.control}
                name="username"
                render={({ field }) => (
                  <FormItem>
                    <FormLabel>아이디</FormLabel>
                    <FormControl>
                      <Input placeholder="user01 (4~10자, 소문자+숫자)" {...field} />
                    </FormControl>
                    <FormMessage />
                  </FormItem>
                )}
              />
              <FormField
                control={form.control}
                name="name"
                render={({ field }) => (
                  <FormItem>
                    <FormLabel>이름</FormLabel>
                    <FormControl>
                      <Input placeholder="홍길동" {...field} />
                    </FormControl>
                    <FormMessage />
                  </FormItem>
                )}
              />
            </div>

            <FormField
              control={form.control}
              name="email"
              render={({ field }) => (
                <FormItem>
                  <FormLabel>이메일</FormLabel>
                  <FormControl>
                    <Input type="email" placeholder="user@example.com" {...field} />
                  </FormControl>
                  <FormMessage />
                </FormItem>
              )}
            />

            <FormField
              control={form.control}
              name="password"
              render={({ field }) => (
                <FormItem>
                  <FormLabel>비밀번호</FormLabel>
                  <FormControl>
                    <Input type="password" placeholder="대소문자+숫자+특수문자 8~15자" {...field} />
                  </FormControl>
                  <FormMessage />
                </FormItem>
              )}
            />

            <div className="grid grid-cols-2 gap-4">
              <FormField
                control={form.control}
                name="phone"
                render={({ field }) => (
                  <FormItem>
                    <FormLabel>전화번호 (선택)</FormLabel>
                    <FormControl>
                      <Input placeholder="010-1234-5678" {...field} />
                    </FormControl>
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
                      <Input placeholder="U0123456789" {...field} />
                    </FormControl>
                    <FormMessage />
                  </FormItem>
                )}
              />
            </div>

            <FormField
              control={form.control}
              name="role"
              render={({ field }) => (
                <FormItem>
                  <FormLabel>권한</FormLabel>
                  <Select onValueChange={field.onChange} value={field.value}>
                    <FormControl>
                      <SelectTrigger>
                        <SelectValue placeholder="권한 선택" />
                      </SelectTrigger>
                    </FormControl>
                    <SelectContent>
                      {roleOptions.map((role) => (
                        <SelectItem key={role.value} value={role.value}>
                          {role.label}
                        </SelectItem>
                      ))}
                    </SelectContent>
                  </Select>
                  <FormMessage />
                </FormItem>
              )}
            />

            {(selectedRole === "HUB_MANAGER" || selectedRole === "DELIVERY_MANAGER") && (
              <FormField
                control={form.control}
                name="hubId"
                render={({ field }) => (
                  <FormItem>
                    <FormLabel>소속 허브 (선택)</FormLabel>
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

            {selectedRole === "COMPANY_MANAGER" && (
              <FormField
                control={form.control}
                name="companyId"
                render={({ field }) => (
                  <FormItem>
                    <FormLabel>소속 업체 (선택)</FormLabel>
                    <Select onValueChange={field.onChange} value={field.value || undefined}>
                      <FormControl>
                        <SelectTrigger>
                          <SelectValue placeholder="업체 선택" />
                        </SelectTrigger>
                      </FormControl>
                      <SelectContent>
                        {companies.map((c) => (
                          <SelectItem key={c.companyId} value={c.companyId}>
                            {c.companyName}
                          </SelectItem>
                        ))}
                      </SelectContent>
                    </Select>
                    <FormMessage />
                  </FormItem>
                )}
              />
            )}

            <DialogFooter className="pt-4">
              <Button type="button" variant="outline" onClick={() => setOpen(false)}>취소</Button>
              <Button type="submit" disabled={isSubmitting}>
                {isSubmitting ? "처리 중..." : "가입 신청"}
              </Button>
            </DialogFooter>
          </form>
        </Form>
      </DialogContent>
    </Dialog>
  )
}
