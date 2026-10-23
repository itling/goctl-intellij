package cn.xiaoheiban;

import cn.xiaoheiban.psi.ApiFile;
import cn.xiaoheiban.psi.ApiBodyExpr;
import com.intellij.psi.PsiElement;
import com.intellij.psi.util.PsiTreeUtil;
import com.intellij.testFramework.fixtures.BasePlatformTestCase;

/** Reproduces shared request/response types in split App/Web API definitions. */
public class ApiTypeResolutionTest extends BasePlatformTestCase {
    private static final String ROUTE = """
            service br-api {
                @handler VehicleTemplatePage
                get /templates (VehicleTemplatePageReq) returns (VehicleTemplatePageResp)
            }
            """;
    private static final String TYPES = """
            type (
                VehicleTemplatePageReq { Page int64 }
                VehicleTemplatePageResp { Total int64 }
            )
            """;

    public void testExplicitImportWithoutLocalTypes() {
        myFixture.addFileToProject("docs/types/vehicle_template.api", TYPES);
        ApiFile app = file("docs/app/vehicle.api", "import \"../types/vehicle_template.api\"\n" + ROUTE);
        assertTypesResolve(app);
    }

    public void testTypesSharedByEntryPointImports() {
        ApiFile web = file("docs/web/vehicle.api", TYPES + ROUTE);
        ApiFile app = file("docs/app/vehicle.api", ROUTE);
        myFixture.addFileToProject("br.api", "import (\"docs/web/vehicle.api\" \"docs/app/vehicle.api\")");
        assertTypesResolve(app);
        assertTypesResolve(web);
    }

    public void testTypesOnlyEntryPointImports() {
        myFixture.addFileToProject("docs/types/vehicle_template.api", TYPES);
        ApiFile app = file("docs/app/vehicle.api", ROUTE);
        myFixture.addFileToProject("br.api", "import (\"docs/types/vehicle_template.api\" \"docs/app/vehicle.api\")");
        assertTypesResolve(app);
        assertNull(ApiPsiUtil.findTypeDeclaration(app, "MissingType"));
    }

    public void testSharedTypesHighlightAndNavigateToTheirDeclaration() {
        ApiFile types = file("types.api", TYPES);
        ApiFile app = file("docs/app/vehicle.api", ROUTE);
        myFixture.addFileToProject("br.api", "import (\"types.api\" \"docs/app/vehicle.api\")");
        myFixture.configureFromExistingVirtualFile(app.getVirtualFile());
        myFixture.checkHighlighting(false, false, false);

        ApiBodyExpr request = PsiTreeUtil.findChildOfType(app, ApiBodyExpr.class);
        assertNotNull(request);
        PsiElement declaration = new ApiGotoDeclarationHandler()
                .getGotoDeclarationTarget(request.getIdent(), myFixture.getEditor());
        assertNotNull(declaration);
        assertEquals(types, declaration.getContainingFile());
    }

    public void testUnrelatedEntryPointDoesNotLeakTypes() {
        ApiFile app = file("docs/app/vehicle.api", ROUTE);
        myFixture.addFileToProject("other/types.api", TYPES);
        myFixture.addFileToProject("other.api", "import \"other/types.api\"");
        assertNull(ApiPsiUtil.findTypeDeclaration(app, "VehicleTemplatePageReq"));
    }

    public void testTransitiveImportsAndCyclesTerminate() {
        ApiFile app = file("docs/app/vehicle.api", "import \"../types/api.api\"\n" + ROUTE);
        myFixture.addFileToProject("docs/types/api.api", "import \"../../br.api\"\n" + TYPES);
        myFixture.addFileToProject("br.api", "import \"docs/app/vehicle.api\"");
        assertTypesResolve(app);
    }

    public void testNewEntryPointIsRecognizedAfterInitialLookup() {
        ApiFile app = file("docs/app/vehicle.api", ROUTE);
        assertNull(ApiPsiUtil.findTypeDeclaration(app, "VehicleTemplatePageReq"));
        myFixture.addFileToProject("types.api", TYPES);
        myFixture.addFileToProject("br.api", "import (\"types.api\" \"docs/app/vehicle.api\")");
        assertTypesResolve(app);
    }

    private ApiFile file(String path, String text) {
        return assertInstanceOf(myFixture.addFileToProject(path, text), ApiFile.class);
    }

    private void assertTypesResolve(ApiFile file) {
        assertNotNull(ApiPsiUtil.findTypeDeclaration(file, "VehicleTemplatePageReq"));
        assertNotNull(ApiPsiUtil.findTypeDeclaration(file, "VehicleTemplatePageResp"));
    }
}
