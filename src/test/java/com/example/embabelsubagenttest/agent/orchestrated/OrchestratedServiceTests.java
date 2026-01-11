package com.example.embabelsubagenttest.agent.orchestrated;

import com.example.embabelsubagenttest.agent.orchestrated.OrchestratedTypes.*;
import com.example.embabelsubagenttest.service.ColorEditService;
import com.example.embabelsubagenttest.service.NameEditService;
import com.example.embabelsubagenttest.service.RouteEditService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

class OrchestratedServiceTests {

    private StudioConsole mockConsole;
    private NameEditService nameEditService;
    private ColorEditService colorEditService;
    private RouteEditService routeEditService;

    @BeforeEach
    void setUp() {
        mockConsole = Mockito.mock(StudioConsole.class);
        nameEditService = new NameEditService();
        colorEditService = new ColorEditService();
        routeEditService = new RouteEditService();
    }

    @Test
    void testEditPlanIsEmpty() {
        EditPlan emptyPlan = new EditPlan(null, List.of(), null);
        assertTrue(emptyPlan.isEmpty());

        EditPlan planWithName = new EditPlan(List.of(new NameEditTask(1, "Vocals")), null, null);
        assertFalse(planWithName.isEmpty());

        EditPlan planWithColor = new EditPlan(null, List.of(new ColorEditTask(1, "#FF0000")), null);
        assertFalse(planWithColor.isEmpty());

        EditPlan planWithRoute = new EditPlan(null, null, List.of(new RouteEditTask(1, "Bus 1")));
        assertFalse(planWithRoute.isEmpty());
    }

    @Test
    void testNameEditServiceSuccess() {
        List<NameEditTask> tasks = List.of(new NameEditTask(1, "Drums"));
        
        List<NameEditResult> results = nameEditService.applyEdits(tasks, mockConsole);

        assertEquals(1, results.size());
        assertTrue(results.get(0).isSuccess());
        assertEquals("Channel 1 renamed to 'Drums'", results.get(0).message());
        verify(mockConsole).setChannelName(1, "Drums");
    }

    @Test
    void testNameEditServiceError() {
        doThrow(new RuntimeException("Console Error")).when(mockConsole).setChannelName(anyInt(), anyString());
        List<NameEditTask> tasks = List.of(new NameEditTask(1, "Drums"));

        List<NameEditResult> results = nameEditService.applyEdits(tasks, mockConsole);

        assertEquals(1, results.size());
        assertFalse(results.get(0).isSuccess());
        assertTrue(results.get(0).errorMessage().contains("Console Error"));
    }

    @Test
    void testColorEditServiceSuccess() {
        List<ColorEditTask> tasks = List.of(new ColorEditTask(2, "#00FF00"));

        List<ColorEditResult> results = colorEditService.applyEdits(tasks, mockConsole);

        assertEquals(1, results.size());
        assertTrue(results.get(0).isSuccess());
        verify(mockConsole).setChannelColor(2, "#00FF00");
    }

    @Test
    void testColorEditServiceError() {
        doThrow(new RuntimeException("Invalid Color")).when(mockConsole).setChannelColor(anyInt(), anyString());
        List<ColorEditTask> tasks = List.of(new ColorEditTask(2, "Greeeen"));

        List<ColorEditResult> results = colorEditService.applyEdits(tasks, mockConsole);

        assertEquals(1, results.size());
        assertFalse(results.get(0).isSuccess());
        assertTrue(results.get(0).errorMessage().contains("Invalid Color"));
    }

    @Test
    void testRouteEditServiceSuccess() {
        List<RouteEditTask> tasks = List.of(new RouteEditTask(3, "Main Out"));

        List<RouteEditResult> results = routeEditService.applyEdits(tasks, mockConsole);

        assertEquals(1, results.size());
        assertTrue(results.get(0).isSuccess());
        verify(mockConsole).setChannelRoute(3, "Main Out");
    }

    @Test
    void testRouteEditServiceError() {
        doThrow(new RuntimeException("Bus Busy")).when(mockConsole).setChannelRoute(anyInt(), anyString());
        List<RouteEditTask> tasks = List.of(new RouteEditTask(3, "Aux 1"));

        List<RouteEditResult> results = routeEditService.applyEdits(tasks, mockConsole);

        assertEquals(1, results.size());
        assertFalse(results.get(0).isSuccess());
        assertTrue(results.get(0).errorMessage().contains("Bus Busy"));
    }
}
