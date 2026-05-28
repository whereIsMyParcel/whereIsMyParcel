"use client";

import { useState, useEffect } from "react";
import { useForm } from "react-hook-form";
import { zodResolver } from "@hookform/resolvers/zod";
import * as z from "zod";
import { fetchApi } from "@/lib/api";
import {
  Dialog,
  DialogContent,
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
import { Input } from "@/components/ui/input";
import { Button } from "@/components/ui/button";
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from "@/components/ui/select";
import { Plus } from "lucide-react";
import { toast } from "sonner";

const formSchema = z.object({
  originHubId: z.string().min(1, "출발 허브를 선택해주세요."),
  destinationHubId: z.string().min(1, "도착 허브를 선택해주세요."),
  distance: z.coerce.number().positive("거리는 0보다 커야 합니다."),
  duration: z.coerce.number().positive("소요 시간은 0보다 커야 합니다."),
}).refine(data => data.originHubId !== data.destinationHubId, {
  message: "출발 허브와 도착 허브는 같을 수 없습니다.",
  path: ["destinationHubId"]
});

interface CreateRouteModalProps {
  onSuccess: () => void;
}

export function CreateRouteModal({ onSuccess }: CreateRouteModalProps) {
  const [open, setOpen] = useState(false);
  const [hubs, setHubs] = useState<any[]>([]);

  const form = useForm<z.infer<typeof formSchema>>({
    resolver: zodResolver(formSchema),
    defaultValues: {
      originHubId: "",
      destinationHubId: "",
      distance: 0,
      duration: 0,
    },
  });

  useEffect(() => {
    if (open) {
      fetchApi<any>('/hubs?size=50')
        .then(res => setHubs(res?.content || []))
        .catch(err => console.error("Failed to load hubs", err));
    }
  }, [open]);

  async function onSubmit(values: z.infer<typeof formSchema>) {
    try {
      await fetchApi("/hub-routes", {
        method: "POST",
        body: JSON.stringify(values),
      });
      toast.success("이동 경로가 성공적으로 추가되었습니다.");
      setOpen(false);
      form.reset();
      onSuccess();
    } catch (error: any) {
      toast.error(`경로 추가 실패: ${error.message}`);
    }
  }

  return (
    <Dialog open={open} onOpenChange={setOpen}>
      <DialogTrigger asChild>
        <Button className="bg-primary hover:bg-primary/90">
          <Plus className="w-4 h-4 mr-2" />
          경로 추가
        </Button>
      </DialogTrigger>
      <DialogContent className="sm:max-w-[500px]">
        <DialogHeader>
          <DialogTitle>새 이동 경로 추가</DialogTitle>
        </DialogHeader>
        <Form {...form}>
          <form onSubmit={form.handleSubmit(onSubmit)} className="space-y-4">
            
            <FormField
              control={form.control}
              name="originHubId"
              render={({ field }) => (
                <FormItem>
                  <FormLabel>출발 허브</FormLabel>
                  <Select onValueChange={field.onChange} defaultValue={field.value}>
                    <FormControl>
                      <SelectTrigger>
                        <SelectValue placeholder="출발 허브 선택" />
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
              name="destinationHubId"
              render={({ field }) => (
                <FormItem>
                  <FormLabel>도착 허브</FormLabel>
                  <Select onValueChange={field.onChange} defaultValue={field.value}>
                    <FormControl>
                      <SelectTrigger>
                        <SelectValue placeholder="도착 허브 선택" />
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

            <div className="grid grid-cols-2 gap-4">
              <FormField
                control={form.control}
                name="distance"
                render={({ field }) => (
                  <FormItem>
                    <FormLabel>거리 (km)</FormLabel>
                    <FormControl>
                      <Input type="number" step="0.1" placeholder="0.0" {...field} />
                    </FormControl>
                    <FormMessage />
                  </FormItem>
                )}
              />

              <FormField
                control={form.control}
                name="duration"
                render={({ field }) => (
                  <FormItem>
                    <FormLabel>소요 시간 (분)</FormLabel>
                    <FormControl>
                      <Input type="number" placeholder="0" {...field} />
                    </FormControl>
                    <FormMessage />
                  </FormItem>
                )}
              />
            </div>

            <div className="flex justify-end pt-4">
              <Button type="submit">경로 저장</Button>
            </div>
          </form>
        </Form>
      </DialogContent>
    </Dialog>
  );
}
