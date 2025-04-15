package ch.sebpiller.spi.toolkit.aop;


import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.reflect.MethodSignature;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;

import java.lang.reflect.Method;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class AutoDeprecateAspectTest {

    @Test
    void test_not_a_response_entity() throws Throwable {
        var ada = new AutoDeprecateAspect();

        var pjp = mock(ProceedingJoinPoint.class);
        var mock = mock(MethodSignature.class);
        when(mock.getMethod()).thenReturn(mock(Method.class));
        when(pjp.getSignature()).thenReturn(mock);

        var someResponse = new Object();
        when(pjp.proceed()).thenReturn(someResponse);

        var response = ada.addDeprecatedHeaderToSpringResponseIfRequired(pjp);
        Assertions.assertThat(response).isNotNull().isSameAs(someResponse);
    }

    @Test
    void test_not_deprecated() throws Throwable {
        var ada = new AutoDeprecateAspect();

        var pjp = mock(ProceedingJoinPoint.class);
        var mock = mock(MethodSignature.class);
        when(mock.getMethod()).thenReturn(mock(Method.class));
        when(pjp.getSignature()).thenReturn(mock);

        var someResponse = new ResponseEntity<>(HttpStatusCode.valueOf(200));
        when(pjp.proceed()).thenReturn(someResponse);

        var response = ada.addDeprecatedHeaderToSpringResponseIfRequired(pjp);
        Assertions.assertThat(response).isNotNull().isSameAs(someResponse);
    }

    @Test
    void test_vanilla() throws Throwable {
        // TODO implement
        /*var ada = new AutoDeprecateAspect();

        var pjp = mock(ProceedingJoinPoint.class);
        var mock = mock(MethodSignature.class);

        var mock1 = mock(Method.class);
        ///when(mock1.getDeclaringClass()).thenReturn(Object.class);
        //getInterfaces

        when(mock.getMethod()).thenReturn(mock1);
        when(pjp.getSignature()).thenReturn(mock);

        var someResponse = new ResponseEntity<>(HttpStatusCode.valueOf(200));
        when(pjp.proceed()).thenReturn(someResponse);

        var response = ada.addDeprecatedHeaderToSpringResponseIfRequired(pjp);
        //Assertions.assertThat(response).isNotNull().isSameAs(someResponse);*/
    }

}