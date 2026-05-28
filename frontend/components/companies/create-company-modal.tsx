"use client";

import { useState, useEffect } from "react";
import { useForm } from "react-hook-form";
import { zodResolver } from "@hookform/resolvers/zod";
import * as z from "zod";
import { useDaumPostcodePopup } from "react-daum-postcode";
import { toast } from "sonner";
import { Plus, Search } from "lucide-react";

import { fetchApi } from "@/lib/api";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import {
  Dialog,
  DialogContent,
  DialogDescription,
  DialogFooter,
  DialogHeader,
  DialogTitle,
  DialogTrigger,
} from "@/components/ui/dialog";
import {
  Form,
  FormControl,
  FormField,
  FormItem,
  FormLabel,
  FormMessage,
} from "@/components/ui/form";
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from "@/components/ui/select";

const formSchema = z.object({
  hubId: z.string().uuid({ message: "소속 허브를 선택해주세요." }),
  companyType: z.enum(["RECEIVER", "SUPPLIER"], { message: "업체 타입을 선택해주세요." }),
  companyName: z.string().min(1, { message: "업체 이름을 입력해주세요." }).max(100),
  businessNumber: z.string().regex(/^\d{3}-\d{2}-\d{5}$/, { message: "사업자등록번호 형식이 올바르지 않습니다. (예: 123-45-67890)" }),
  managerName: z.string().min(1, { message: "사업자 명을 입력해주세요." }).max(50),
  managerPhone: z.string().regex(/^\d{2,3}-\d{3,4}-\d{4}$/, { message: "전화번호 형식이 올바르지 않습니다. (예: 010-1234-5678)" }),
  zipCode: z.string().regex(/^\d{5}$/, { message: "우편번호는 숫자 5자리여야 합니다." }),
  address: z.string().min(1, { message: "주소를 입력해주세요." }).max(255),
  addressDetail: z.string().max(255).optional().default(""),
});

export function CreateCompanyModal({ onSuccess }: { onSuccess: () => void }) {
  const [open, setOpen] = useState(false);
  const [isSubmitting, setIsSubmitting] = useState(false);
  const [hubs, setHubs] = useState<any[]>([]);
  const openPostcode = useDaumPostcodePopup();

  useEffect(() => {
    if (open && hubs.length === 0) {
      fetchApi<any>('/hubs?size=50').then((pageData) => {
        setHubs(pageData?.content || []);
      }).catch((err) => {
        console.error(err);
        toast.error("허브 목록을 불러오지 못했습니다.");
      });
    }
  }, [open, hubs.length]);

  const form = useForm<z.infer<typeof formSchema>>({
    resolver: zodResolver(formSchema),
    defaultValues: {
      hubId: "",
      companyType: "SUPPLIER",
      companyName: "",
      businessNumber: "",
      managerName: "",
      managerPhone: "",
      zipCode: "",
      address: "",
      addressDetail: "",
    },
  });

  const handleComplete = (data: any) => {
    let fullAddress = data.address;
    let extraAddress = "";

    if (data.addressType === "R") {
      if (data.bname !== "") {
        extraAddress += data.bname;
      }
      if (data.buildingName !== "") {
        extraAddress += extraAddress !== "" ? `, ${data.buildingName}` : data.buildingName;
      }
      fullAddress += extraAddress !== "" ? ` (${extraAddress})` : "";
    }

    form.setValue("zipCode", data.zonecode, { shouldValidate: true });
    form.setValue("address", fullAddress, { shouldValidate: true });
  };

  const handleSearchAddress = () => {
    openPostcode({ onComplete: handleComplete });
  };

  async function onSubmit(values: z.infer<typeof formSchema>) {
    try {
      setIsSubmitting(true);
      await fetchApi("/companies", {
        method: "POST",
        body: JSON.stringify(values),
      });
      toast.success("업체가 성공적으로 추가되었습니다.");
      form.reset();
      setOpen(false);
      onSuccess();
    } catch (error: any) {
      toast.error(error.message || "업체 추가에 실패했습니다.");
    } finally {
      setIsSubmitting(false);
    }
  }

  return (
    <Dialog open={open} onOpenChange={(val) => {
      setOpen(val);
      if (!val) form.reset();
    }}>
      <DialogTrigger asChild>
        <Button className="bg-primary hover:bg-primary/90">
          <Plus className="w-4 h-4 mr-2" />
          업체 추가
        </Button>
      </DialogTrigger>
      <DialogContent className="sm:max-w-[600px] max-h-[90vh] overflow-y-auto">
        <DialogHeader>
          <DialogTitle>새 업체 추가</DialogTitle>
          <DialogDescription>
            새로운 생산업체 또는 수령업체를 등록합니다.
          </DialogDescription>
        </DialogHeader>
        <Form {...form}>
          <form onSubmit={form.handleSubmit(onSubmit)} className="space-y-4 py-4">
            <div className="grid grid-cols-2 gap-4">
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
              <FormField
                control={form.control}
                name="companyType"
                render={({ field }) => (
                  <FormItem>
                    <FormLabel>업체 타입</FormLabel>
                    <Select onValueChange={field.onChange} value={field.value}>
                      <FormControl>
                        <SelectTrigger>
                          <SelectValue placeholder="타입 선택" />
                        </SelectTrigger>
                      </FormControl>
                      <SelectContent>
                        <SelectItem value="SUPPLIER">생산업체</SelectItem>
                        <SelectItem value="RECEIVER">수령업체</SelectItem>
                      </SelectContent>
                    </Select>
                    <FormMessage />
                  </FormItem>
                )}
              />
            </div>
            
            <FormField
              control={form.control}
              name="companyName"
              render={({ field }) => (
                <FormItem>
                  <FormLabel>업체 이름</FormLabel>
                  <FormControl>
                    <Input placeholder="예: 스파르타 물류" {...field} />
                  </FormControl>
                  <FormMessage />
                </FormItem>
              )}
            />

            <div className="grid grid-cols-2 gap-4">
              <FormField
                control={form.control}
                name="businessNumber"
                render={({ field }) => (
                  <FormItem>
                    <FormLabel>사업자등록번호</FormLabel>
                    <FormControl>
                      <Input placeholder="123-45-67890" {...field} />
                    </FormControl>
                    <FormMessage />
                  </FormItem>
                )}
              />
              <FormField
                control={form.control}
                name="managerName"
                render={({ field }) => (
                  <FormItem>
                    <FormLabel>담당자 이름</FormLabel>
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
              name="managerPhone"
              render={({ field }) => (
                <FormItem>
                  <FormLabel>담당자 전화번호</FormLabel>
                  <FormControl>
                    <Input placeholder="010-1234-5678" {...field} />
                  </FormControl>
                  <FormMessage />
                </FormItem>
              )}
            />

            <div className="space-y-2">
              <FormField
                control={form.control}
                name="zipCode"
                render={({ field }) => (
                  <FormItem>
                    <FormLabel>주소</FormLabel>
                    <div className="flex gap-2">
                      <FormControl>
                        <Input placeholder="우편번호" readOnly {...field} className="w-32" />
                      </FormControl>
                      <Button type="button" variant="secondary" onClick={handleSearchAddress}>
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
                      <Input placeholder="상세 주소를 입력해주세요" {...field} />
                    </FormControl>
                    <FormMessage />
                  </FormItem>
                )}
              />
            </div>

            <DialogFooter className="pt-4">
              <Button type="button" variant="outline" onClick={() => setOpen(false)}>
                취소
              </Button>
              <Button type="submit" disabled={isSubmitting}>
                {isSubmitting ? "저장 중..." : "저장"}
              </Button>
            </DialogFooter>
          </form>
        </Form>
      </DialogContent>
    </Dialog>
  );
}
