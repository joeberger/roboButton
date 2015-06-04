package com.ndipatri.roboButton.services;

@RunWith(RobolectricGradleTestRunner.class)
@Config(manifest = "src/main/AndroidManifest.xml", constants = BuildConfig.class, emulateSdk = 19)
public class MonitoringServiceTest {

    @Test
    public void test() {
        assertTrue(true);
    }
}
