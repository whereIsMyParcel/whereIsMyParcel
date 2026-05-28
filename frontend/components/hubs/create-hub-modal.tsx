"use client";

import { useState } from "react";
import { useForm } from "react-hook-form";
import { zodResolver } from "@hookform/resolvers/zod";
import * as z from "zod";
import { useDaumPostcodePopup } from "react-daum-postcode";
import { toast } from "sonner";
import { Plus, MapPin, Search } from "lucide-react";

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

const formSchema = z.object({
  name: z.string().min(2, { message: "허브 이름은 최소 2글자 이상이어야 합니다." }),
  address: z.string().min(5, { message: "정확한 주소를 입력해주세요." }),
  latitude: z.coerce.number().min(-90).max(90, { message: "정확한 위도 값을 입력해주세요." }),
  longitude: z.coerce.number().min(-180).max(180, { message: "정확한 경도 값을 입력해주세요." }),
});

export function CreateHubModal({ onSuccess }: { onSuccess: () => void }) {
  const [open, setOpen] = useState(false);
  const [isSubmitting, setIsSubmitting] = useState(false);
  const openPostcode = useDaumPostcodePopup();

  const form = useForm<z.infer<typeof formSchema>>({
    resolver: zodResolver(formSchema),
    defaultValues: {
      name: "",
      address: "",
      latitude: 0,
      longitude: 0,
    },
  });

  // 카카오 우편번호 서비스 완료 콜백
  const handleComplete = async (data: any) => {
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

    form.setValue("address", fullAddress);

    // 좌표 변환 (Geocoding)
    // 클라이언트에서는 apiKey 여부를 체크하지 않고 서버 라우트(/api/geocode)만 믿고 호출합니다.

    try {
      const response = await fetch(`/api/geocode?address=${encodeURIComponent(fullAddress)}`);
      
      const result = await response.json();
      
      if (!response.ok) {
        throw new Error(result.error || "Geocoding failed");
      }
      
      if (result.documents && result.documents.length > 0) {
        const { x, y } = result.documents[0];
        form.setValue("latitude", parseFloat(y), { shouldDirty: true, shouldValidate: true });
        form.setValue("longitude", parseFloat(x), { shouldDirty: true, shouldValidate: true });
        toast.success("주소를 통해 위도/경도를 성공적으로 변환했습니다.");
      } else {
        toast.warning("주소에 해당하는 좌표를 찾을 수 없습니다.");
      }
    } catch (error: any) {
      console.error("Geocoding failed:", error);
      toast.error(`좌표 변환 실패: ${error.message}`);
    }
  };

  const handleSearchAddress = () => {
    openPostcode({ onComplete: handleComplete });
  };

  async function onSubmit(values: z.infer<typeof formSchema>) {
    try {
      setIsSubmitting(true);
      await fetchApi("/hubs", {
        method: "POST",
        body: JSON.stringify(values),
      });
      toast.success("허브가 성공적으로 추가되었습니다.");
      form.reset();
      setOpen(false);
      onSuccess(); // 테이블 새로고침
    } catch (error: any) {
      toast.error(error.message || "허브 추가에 실패했습니다.");
    } finally {
      setIsSubmitting(false);
    }
  }

  return (
    <Dialog open={open} onOpenChange={setOpen}>
      <DialogTrigger asChild>
        <Button className="bg-primary hover:bg-primary/90">
          <Plus className="w-4 h-4 mr-2" />
          허브 추가
        </Button>
      </DialogTrigger>
      <DialogContent className="sm:max-w-[500px]">
        <DialogHeader>
          <DialogTitle>새 허브 추가</DialogTitle>
          <DialogDescription>
            스파르타 물류의 새로운 허브 정보를 입력해주세요.
          </DialogDescription>
        </DialogHeader>
        <Form {...form}>
          <form onSubmit={form.handleSubmit(onSubmit)} className="space-y-4 py-4">
            <FormField
              control={form.control}
              name="name"
              render={({ field }) => (
                <FormItem>
                  <FormLabel>허브 이름</FormLabel>
                  <FormControl>
                    <Input placeholder="예: 서울특별시 센터" {...field} />
                  </FormControl>
                  <FormMessage />
                </FormItem>
              )}
            />
            <div className="space-y-2">
              <FormField
                control={form.control}
                name="address"
                render={({ field }) => (
                  <FormItem>
                    <FormLabel>주소</FormLabel>
                    <div className="flex gap-2">
                      <FormControl>
                        <Input placeholder="주소를 검색해주세요" readOnly {...field} />
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
            </div>
            <div className="grid grid-cols-2 gap-4">
              <FormField
                control={form.control}
                name="latitude"
                render={({ field }) => (
                  <FormItem>
                    <FormLabel>위도 (Latitude)</FormLabel>
                    <FormControl>
                      <Input type="number" step="0.0000001" placeholder="37.5665" {...field} />
                    </FormControl>
                    <FormMessage />
                  </FormItem>
                )}
              />
              <FormField
                control={form.control}
                name="longitude"
                render={({ field }) => (
                  <FormItem>
                    <FormLabel>경도 (Longitude)</FormLabel>
                    <FormControl>
                      <Input type="number" step="0.0000001" placeholder="126.9780" {...field} />
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
