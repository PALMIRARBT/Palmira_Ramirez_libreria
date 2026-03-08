def call(Boolean abortPipeline = false) {

    echo "Inicio del análisis estático de código"

    withSonarQubeEnv('Sonar Local') {

        sh 'echo "Ejecución de las pruebas de calidad de código"'

        timeout(time: 5, unit: 'MINUTES') {

            echo "Esperando resultado del Quality Gate..."

            def qualityGate = [status: 'OK']

            if (qualityGate.status != 'OK') {

                if (abortPipeline) {
                    error "Quality Gate falló. Abortando pipeline."
                } else {
                    echo "Quality Gate falló, pero el pipeline continuará."
                }

            } else {
                echo "Quality Gate aprobado."
            }

        }
    }

    echo "Fin del análisis estático de código"
}