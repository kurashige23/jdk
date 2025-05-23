/*
 * @test    /nodynamiccopyright/
 * @bug     6399361
 * @summary java.lang.Override specification should be revised
 * @author  Peter von der Ahé
 * @compile/fail/ref=Private.out -XDrawDiagnostics  Private.java
 */

public class Private {
    private void m() {}
}

class Bar extends Private {
    @Override
    private void m() {}
}
