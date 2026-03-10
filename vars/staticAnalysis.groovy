def call(Boolean abortPipeline = false, String branchName = null, Boolean waitForQG = false) {

    echo "Inicio del análisis estático de código"

    // Leer variable de entorno que también puede forzar el abort (ej: ABORT_ON_QG=true)
    def envAbort = (env.ABORT_ON_QG ?: 'false').toString().toLowerCase() == 'true'
    def alwaysAbort = abortPipeline || envAbort

    // Determinar nombre de la rama
    def rawBranch = branchName ?: (env.BRANCH_NAME ?: (env.GIT_BRANCH ?: 'unknown'))
    def branch = rawBranch.replaceAll(/^refs\\/heads\\//, '').replaceAll(/^origin\\//, '')

    // Usamos withSonarQubeEnv para inyectar las variables del servidor Sonar
    withSonarQubeEnv('Sonar Local') {

        // Ejecutar el análisis (intenta sonar-scanner, si no existe simula)
        sh '''
            if command -v sonar-scanner >/dev/null 2>&1; then
                sonar-scanner
            else
                echo "sonar-scanner no encontrado: simulando análisis de SonarQube"
            fi
        '''

        if (waitForQG) {
            // Esperar el quality gate dentro de un timeout de 5 minutos
            timeout(time: 5, unit: 'MINUTES') {
                echo "Esperando resultado del Quality Gate..."
                def qg = waitForQualityGate()
                echo "Quality Gate status: ${qg.status}"
                if (qg.status != 'OK') {
                    if (alwaysAbort) {
                        error "Quality Gate falló. Abortando pipeline (politica: alwaysAbort=true). Branch: ${branch}"
                    } else {
                        // Si no debe abortar por parámetro, la heurística por rama aplica
                        if (branch == 'master' || branch == 'main' || branch.startsWith('hotfix')) {
                            error "Quality Gate falló en rama crítica (${branch}). Abortando pipeline."
                        } else {
                            echo "Quality Gate falló, pero el pipeline continuará (rama: ${branch})."
                        }
                    }
                } else {
                    echo "Quality Gate aprobado."
                }
            }
        } else {
            echo "No se espera el Quality Gate (modo rápido)."
        }
    }

    echo "Fin del análisis estático de código"
}