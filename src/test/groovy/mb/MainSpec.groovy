package mb

import mb.argumentprocessing.ArgumentsMerge
import mb.usecase.ArgumentsValidatorInteractor

import spock.lang.Specification

class MainSpec extends Specification {

    Main main

    def setup() {
        main = new Main()
        main.argumentsMerge = Mock(ArgumentsMerge)
        main.argumentsValidatorInteractor = Mock(ArgumentsValidatorInteractor)
    }

}
