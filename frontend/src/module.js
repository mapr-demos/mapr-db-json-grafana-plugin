import {MapRDBJSONTableDatasource} from './datasource';
import {MapRDBJSONTableDatasourceQueryCtrl} from './query_ctrl';

class GenericConfigCtrl {
}
GenericConfigCtrl.templateUrl = 'partials/config.html';

class GenericQueryOptionsCtrl {
}
GenericQueryOptionsCtrl.templateUrl = 'partials/query.options.html';

class GenericAnnotationsQueryCtrl {
}
GenericAnnotationsQueryCtrl.templateUrl = 'partials/annotations.editor.html'

export {
    MapRDBJSONTableDatasource as Datasource,
    MapRDBJSONTableDatasourceQueryCtrl as QueryCtrl,
    GenericConfigCtrl as ConfigCtrl,
    GenericQueryOptionsCtrl as QueryOptionsCtrl,
    GenericAnnotationsQueryCtrl as AnnotationsQueryCtrl
};
