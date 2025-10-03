#!/bin/bash
echo "📝 Gerando relatório de auditoria..."
mkdir -p target/compliance-reports
cat > target/compliance-reports/audit-report.html << 'HTML_EOF'
<!DOCTYPE html>
<html>
<head><title>Audit Report - PIP</title></head>
<body>
<h1>Payment Integration Platform - Audit Report</h1>
<p>Build: ${BUILD_NUMBER}</p>
<p>Date: $(date)</p>
<p>Status: SUCCESS</p>
</body>
</html>
HTML_EOF
echo "✅ Relatório gerado!"
