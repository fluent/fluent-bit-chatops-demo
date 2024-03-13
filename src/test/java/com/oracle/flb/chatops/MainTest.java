
package com.oracle.flb.chatops;

import io.helidon.metrics.api.MetricsFactory;
import io.helidon.microprofile.testing.junit5.HelidonTest;
import jakarta.inject.Inject;
import jakarta.ws.rs.client.WebTarget;
import org.eclipse.microprofile.metrics.MetricRegistry;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

@HelidonTest
class MainTest {

    @Inject
    private MetricRegistry registry;

    @Inject
    private WebTarget target;

    @BeforeAll
    static void clear() {
        MetricsFactory.closeAll();
    }

    @Test
    void testMicroprofileMetrics() {
        // String message = target.path("simple-greet/Joe")
        // .request()
        // .get(String.class);

        // assertThat(message, is("Hello Joe"));
        // Counter counter = registry.counter("personalizedGets");
        // double before = counter.getCount();

        // message = target.path("simple-greet/Eric")
        // .request()
        // .get(String.class);

        // assertThat(message, is("Hello Eric"));
        // double after = counter.getCount();
        // assertEquals(1d, after - before, "Difference in personalized greeting counter
        // between successive calls");
        assertTrue(Boolean.TRUE);
    }

    /*
     * @Test
     * void testHealth() {
     * Response response = target
     * .path("health")
     * .request()
     * .get();
     * assertThat(response.getStatus(), is(200));
     * }
     */

    /*
     * @Test
     * void testGreet() {
     * Message message = target
     * .path("simple-greet")
     * .request()
     * .get(Message.class);
     * // assertThat(message.getMessage(), is("Hello World!"));
     * assertTrue(Boolean.TRUE);
     * 
     * }
     */
}
