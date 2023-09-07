package ca.bc.gov.educ.pen.nominalroll.api.config;

import ca.bc.gov.educ.pen.nominalroll.api.BaseNominalRollAPITest;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.*;

public class RequestResponseInterceptorTest extends BaseNominalRollAPITest {
  @Autowired
  private RequestResponseInterceptor requestInterceptor;

  @Test
  public void testPreHandle_givenRequest_shouldLogMessage() throws Exception {
    var request = mock(HttpServletRequest.class);
    var response = mock(HttpServletResponse.class);
    when(request.getMethod()).thenReturn("get");
    when(request.getRequestURL()).thenReturn(new StringBuffer("http://localhost"));
    when(request.getQueryString()).thenReturn("pen=\"123456\"");
    assertTrue(requestInterceptor.preHandle(request, response, null));
  }

  @Test
  public void testAfterCompletion_givenSuccessResponse_shouldLogMessage() {
    var request = mock(HttpServletRequest.class);
    var response = mock(HttpServletResponse.class);
    when(response.getStatus()).thenReturn(200);
    requestInterceptor.afterCompletion(request, response, null, null);
    verify(response, atMostOnce()).getStatus();
  }

  @Test
  public void testAfterCompletion_givenFailureResponse_shouldLogMessage() {
    var request = mock(HttpServletRequest.class);
    var response = mock(HttpServletResponse.class);
    when(response.getStatus()).thenReturn(400);
    requestInterceptor.afterCompletion(request, response, null, null);
    verify(response, atMostOnce()).getStatus();
  }
}
