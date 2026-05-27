"use client"

import { useState, useEffect } from "react"
import { useForm } from "react-hook-form"
import { zodResolver } from "@hookform/resolvers/zod"
import * as z from "zod"
import { toast } from "sonner"
import { useDaumPostcodePopup } from "react-daum-postcode"
import { Search } from "lucide-react"

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
import { Input } from "@/components/ui/input"
import { Textarea } from "@/components/ui/textarea"
import { Button } from "@/components/ui/button"
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from "@/components/ui/select"
import { fetchApi } from "@/lib/api"

const orderSchema = z.object({
  companyId: z.string().uuid("업체를 선택해주세요."),
  companyMemberId: z.string().uuid("업체 담당자를 선택해주세요."),
  productId: z.string().uuid("상품을 선택해주세요."),
  productVariantId: z.string().uuid("상품 옵션을 선택해주세요."),
  quantity: z.coerce.number().int().min(1, "수량은 1 이상이어야 합니다."),
  requestMemo: z.string().max(500).optional().default(""),
  requestedDeliveryAt: z.string().min(1, "배송 마감일을 입력해주세요."),
  recipientName: z.string().min(1, "수령인 이름을 입력해주세요.").max(50),
  recipientPhone: z.string().min(1, "수령인 연락처를 입력해주세요.").max(30),
  zipCode: z.string().min(1, "우편번호를 입력해주세요.").max(20),
  address: z.string().min(1, "주소를 입력해주세요.").max(255),
  addressDetail: z.string().max(255).optional().default(""),
})

interface CreateOrderModalProps {
  open: boolean
  onOpenChange: (open: boolean) => void
  onSuccess: () => void
}

export function CreateOrderModal({ open, onOpenChange, onSuccess }: CreateOrderModalProps) {
  const [companies, setCompanies] = useState<any[]>([])
  const [companyMembers, setCompanyMembers] = useState<any[]>([])
  const [products, setProducts] = useState<any[]>([])
  const [variants, setVariants] = useState<any[]>([])
  const [loading, setLoading] = useState(false)
  const [loadingMembers, setLoadingMembers] = useState(false)
  const [loadingVariants, setLoadingVariants] = useState(false)

  const openPostcode = useDaumPostcodePopup()

  const form = useForm<z.infer<typeof orderSchema>>({
    resolver: zodResolver(orderSchema),
    defaultValues: {
      companyId: "",
      companyMemberId: "",
      productId: "",
      productVariantId: "",
      quantity: 1,
      requestMemo: "",
      requestedDeliveryAt: "",
      recipientName: "",
      recipientPhone: "",
      zipCode: "",
      address: "",
      addressDetail: "",
    },
  })

  useEffect(() => {
    if (open) {
      fetchApi<any>('/companies?size=100').then((data) => {
        setCompanies(data?.content || [])
      }).catch(console.error)
      fetchApi<any>('/products?size=100').then((data) => {
        setProducts(data?.content || [])
      }).catch(console.error)
    } else {
      form.reset()
      setCompanyMembers([])
      setVariants([])
    }
  }, [open, form])

  const selectedCompanyId = form.watch("companyId")
  const selectedProductId = form.watch("productId")

  useEffect(() => {
    if (selectedCompanyId) {
      setLoadingMembers(true)
      form.setValue("companyMemberId", "")
      fetchApi<any>(`/companies/${selectedCompanyId}/member?size=100`)
        .then((data) => {
          setCompanyMembers(data?.content || [])
        })
        .catch(() => {
          setCompanyMembers([])
          toast.error("업체 담당자 목록을 불러오지 못했습니다.")
        })
        .finally(() => setLoadingMembers(false))
    }
  }, [selectedCompanyId, form])

  useEffect(() => {
    if (selectedProductId) {
      setLoadingVariants(true)
      form.setValue("productVariantId", "")
      fetchApi<any>(`/products/${selectedProductId}/variants`)
        .then((data: any) => {
          setVariants(Array.isArray(data) ? data : (data?.content || []))
        })
        .catch(() => {
          setVariants([])
          toast.error("상품 옵션을 불러오지 못했습니다.")
        })
        .finally(() => setLoadingVariants(false))
    }
  }, [selectedProductId, form])

  const handleAddressSearch = () => {
    openPostcode({
      onComplete: (data: any) => {
        let fullAddress = data.address
        if (data.addressType === "R") {
          let extra = ""
          if (data.bname) extra += data.bname
          if (data.buildingName) extra += extra ? `, ${data.buildingName}` : data.buildingName
          if (extra) fullAddress += ` (${extra})`
        }
        form.setValue("zipCode", data.zonecode, { shouldValidate: true })
        form.setValue("address", fullAddress, { shouldValidate: true })
      },
    })
  }

  const onSubmit = async (values: z.infer<typeof orderSchema>) => {
    try {
      setLoading(true)
      const payload = {
        companyMemberId: values.companyMemberId,
        requestMemo: values.requestMemo || "",
        requestedDeliveryAt: values.requestedDeliveryAt.length === 16 ? values.requestedDeliveryAt + ":00" : values.requestedDeliveryAt,
        recipientName: values.recipientName,
        recipientPhone: values.recipientPhone,
        zipCode: values.zipCode,
        address: values.address,
        addressDetail: values.addressDetail || "",
        items: [
          {
            productVariantId: values.productVariantId,
            quantity: values.quantity,
          },
        ],
      }
      await fetchApi('/orders', { method: 'POST', body: JSON.stringify(payload) })
      toast.success("주문이 생성되었습니다.")
      onSuccess()
      onOpenChange(false)
    } catch (error: any) {
      toast.error(error.message || "주문 생성에 실패했습니다.")
    } finally {
      setLoading(false)
    }
  }

  return (
    <Dialog open={open} onOpenChange={onOpenChange}>
      <DialogContent className="sm:max-w-[560px] max-h-[90vh] overflow-y-auto">
        <DialogHeader>
          <DialogTitle>주문 생성</DialogTitle>
          <DialogDescription>새로운 배송 주문을 생성합니다.</DialogDescription>
        </DialogHeader>
        <Form {...form}>
          <form onSubmit={form.handleSubmit(onSubmit)} className="space-y-4">

            {/* 업체 선택 */}
            <FormField
              control={form.control}
              name="companyId"
              render={({ field }) => (
                <FormItem>
                  <FormLabel>공급 업체</FormLabel>
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

            {/* 업체 담당자 선택 */}
            <FormField
              control={form.control}
              name="companyMemberId"
              render={({ field }) => (
                <FormItem>
                  <FormLabel>업체 담당자</FormLabel>
                  <Select onValueChange={field.onChange} value={field.value || undefined} disabled={!selectedCompanyId || loadingMembers}>
                    <FormControl>
                      <SelectTrigger>
                        <SelectValue placeholder={loadingMembers ? "불러오는 중..." : "담당자 선택"} />
                      </SelectTrigger>
                    </FormControl>
                    <SelectContent>
                      {companyMembers.length === 0 ? (
                        <SelectItem value="_empty" disabled>담당자가 없습니다.</SelectItem>
                      ) : (
                        companyMembers.map((m) => (
                          <SelectItem key={m.companyMemberId} value={m.companyMemberId}>
                            {m.companyMemberId.slice(0, 8)}... (userId: {m.userId?.slice(0, 8)}...)
                          </SelectItem>
                        ))
                      )}
                    </SelectContent>
                  </Select>
                  <FormMessage />
                </FormItem>
              )}
            />

            {/* 상품 선택 */}
            <FormField
              control={form.control}
              name="productId"
              render={({ field }) => (
                <FormItem>
                  <FormLabel>상품</FormLabel>
                  <Select onValueChange={field.onChange} value={field.value || undefined}>
                    <FormControl>
                      <SelectTrigger>
                        <SelectValue placeholder="상품 선택" />
                      </SelectTrigger>
                    </FormControl>
                    <SelectContent>
                      {products.filter(p => p.status === "ACTIVE").map((p) => (
                        <SelectItem key={p.productId} value={p.productId}>
                          {p.name} ({p.price?.toLocaleString()}원)
                        </SelectItem>
                      ))}
                    </SelectContent>
                  </Select>
                  <FormMessage />
                </FormItem>
              )}
            />

            {/* 상품 옵션(Variant) 선택 */}
            <FormField
              control={form.control}
              name="productVariantId"
              render={({ field }) => (
                <FormItem>
                  <FormLabel>상품 옵션</FormLabel>
                  <Select onValueChange={field.onChange} value={field.value || undefined} disabled={!selectedProductId || loadingVariants}>
                    <FormControl>
                      <SelectTrigger>
                        <SelectValue placeholder={loadingVariants ? "불러오는 중..." : "옵션 선택"} />
                      </SelectTrigger>
                    </FormControl>
                    <SelectContent>
                      {variants.length === 0 ? (
                        <SelectItem value="_empty" disabled>
                          {selectedProductId ? "옵션이 없습니다." : "상품을 먼저 선택해주세요."}
                        </SelectItem>
                      ) : (
                        variants.map((v) => (
                          <SelectItem key={v.variantId} value={v.variantId}>
                            {v.variantName} ({v.variantPrice?.toLocaleString()}원)
                          </SelectItem>
                        ))
                      )}
                    </SelectContent>
                  </Select>
                  <FormMessage />
                </FormItem>
              )}
            />

            {/* 수량 */}
            <FormField
              control={form.control}
              name="quantity"
              render={({ field }) => (
                <FormItem>
                  <FormLabel>수량</FormLabel>
                  <FormControl>
                    <Input type="number" min={1} {...field} />
                  </FormControl>
                  <FormMessage />
                </FormItem>
              )}
            />

            {/* 배송 마감일 */}
            <FormField
              control={form.control}
              name="requestedDeliveryAt"
              render={({ field }) => (
                <FormItem>
                  <FormLabel>납기 마감일</FormLabel>
                  <FormControl>
                    <Input type="datetime-local" {...field} />
                  </FormControl>
                  <FormMessage />
                </FormItem>
              )}
            />

            {/* 수령인 정보 */}
            <div className="grid grid-cols-2 gap-4">
              <FormField
                control={form.control}
                name="recipientName"
                render={({ field }) => (
                  <FormItem>
                    <FormLabel>수령인 이름</FormLabel>
                    <FormControl>
                      <Input placeholder="홍길동" {...field} />
                    </FormControl>
                    <FormMessage />
                  </FormItem>
                )}
              />
              <FormField
                control={form.control}
                name="recipientPhone"
                render={({ field }) => (
                  <FormItem>
                    <FormLabel>수령인 연락처</FormLabel>
                    <FormControl>
                      <Input placeholder="010-1234-5678" {...field} />
                    </FormControl>
                    <FormMessage />
                  </FormItem>
                )}
              />
            </div>

            {/* 배송 주소 */}
            <FormField
              control={form.control}
              name="zipCode"
              render={({ field }) => (
                <FormItem>
                  <FormLabel>배송 주소</FormLabel>
                  <div className="flex gap-2">
                    <FormControl>
                      <Input placeholder="우편번호" readOnly {...field} className="w-32" />
                    </FormControl>
                    <Button type="button" variant="secondary" onClick={handleAddressSearch}>
                      <Search className="w-4 h-4 mr-2" />
                      주소 검색
                    </Button>
                  </div>
                  <FormMessage />
                </FormItem>
              )}
            />
            <FormField
              control={form.control}
              name="address"
              render={({ field }) => (
                <FormItem>
                  <FormControl>
                    <Input placeholder="기본 주소" readOnly {...field} />
                  </FormControl>
                  <FormMessage />
                </FormItem>
              )}
            />
            <FormField
              control={form.control}
              name="addressDetail"
              render={({ field }) => (
                <FormItem>
                  <FormControl>
                    <Input placeholder="상세 주소" {...field} />
                  </FormControl>
                  <FormMessage />
                </FormItem>
              )}
            />

            {/* 요청사항 */}
            <FormField
              control={form.control}
              name="requestMemo"
              render={({ field }) => (
                <FormItem>
                  <FormLabel>요청사항 (선택)</FormLabel>
                  <FormControl>
                    <Textarea placeholder="납품기한, 주의사항 등을 입력해주세요." className="resize-none" {...field} />
                  </FormControl>
                  <FormMessage />
                </FormItem>
              )}
            />

            <DialogFooter>
              <Button type="button" variant="outline" onClick={() => onOpenChange(false)}>취소</Button>
              <Button type="submit" disabled={loading}>{loading ? "생성 중..." : "주문 생성"}</Button>
            </DialogFooter>
          </form>
        </Form>
      </DialogContent>
    </Dialog>
  )
}
