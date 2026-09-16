package com.example.chookjibupadmin.booth.command.application;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;
import com.example.chookjibupadmin.admin.command.application.AdminFestivalRoleService;
import com.example.chookjibupadmin.auth.support.*;
import com.example.chookjibupadmin.booth.command.application.dto.SaveQueuePlanCommand;
import com.example.chookjibupadmin.booth.command.domain.BoothInfo;
import com.example.chookjibupadmin.festival.command.application.FestivalService;
import com.example.chookjibupadmin.operator.command.application.FestivalOperationAccessService;
import com.example.chookjibupadmin.operator.support.FieldStaffPrincipal;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class BoothQueuePlanApplicationServiceTest {
    @Test void fail_Save_StaffCannotEditPlan() {
        var access=mock(FestivalOperationAccessService.class);
        var boothService=mock(BoothInfoService.class);
        var planService=mock(BoothQueuePlanService.class);
        var actor=new FieldStaffPrincipal(1L,2L,"s1",0L);
        var id=UUID.randomUUID();
        when(access.getAuthorizedFestivalId(id,actor)).thenReturn(2L);
        var service=new BoothQueuePlanApplicationService(access,mock(AdminFestivalRoleService.class),boothService,
                planService,mock(BoothQueueMapReader.class),mock(QueueWriteAccess.class),mock(FestivalService.class),
                mock(com.example.chookjibupadmin.map.roadmap.application.FestivalRoadmapService.class));
        assertThatThrownBy(() -> service.save(id,3L,new SaveQueuePlanCommand(null,1,2,null,0,0L),actor))
                .isInstanceOf(RuntimeException.class);
        verifyNoInteractions(planService,boothService);
    }
    @Test void fail_Get_CrossFestivalBooth() {
        var access=mock(FestivalOperationAccessService.class);
        var boothService=mock(BoothInfoService.class);
        var planService=mock(BoothQueuePlanService.class);
        var actor=new AdminPrincipal(1L,"a@mapo.go.kr");
        var id=UUID.randomUUID();
        when(access.getAuthorizedFestivalId(id,actor)).thenReturn(2L);
        when(boothService.getById(3L)).thenReturn(BoothInfo.create(99L,1L,"다른축제"));
        var service=new BoothQueuePlanApplicationService(access,mock(AdminFestivalRoleService.class),boothService,
                planService,mock(BoothQueueMapReader.class),mock(QueueWriteAccess.class),mock(FestivalService.class),
                mock(com.example.chookjibupadmin.map.roadmap.application.FestivalRoadmapService.class));
        assertThatThrownBy(() -> service.get(id,3L,actor)).isInstanceOf(RuntimeException.class);
        verifyNoInteractions(planService);
    }
}
