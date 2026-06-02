package me.onebone.economyapi.task;

import me.onebone.economyapi.EconomyAPI;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class AutoSaveTaskTest {
    @Test
    void onRunDelegatesToOwnerSaveAll() {
        CountingEconomyAPI api = new CountingEconomyAPI();
        AutoSaveTask task = new AutoSaveTask(api);

        task.onRun(20);

        assertEquals(1, api.saveCount);
    }

    private static class CountingEconomyAPI extends EconomyAPI {
        private int saveCount;

        @Override
        public void saveAll() {
            saveCount++;
        }
    }
}
